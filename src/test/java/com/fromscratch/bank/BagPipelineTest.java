package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.persistence.*;
import com.fromscratch.wealth.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Native contents proof -> controller -> forced purchase -> bank deposits -> restart, using isolated saves. */
public class BagPipelineTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private static final int B=ItemID.LOOTING_BAG,O=ItemID.LOOTING_BAG_OPEN,N=ItemID.NATURERUNE,D=ItemID.DRAGON_BONES,C=ItemID.COINS;
	private final Source source=new Source();private final BankOverlayState overlay=new BankOverlayState();private BankView view;
	private Filepath root;private BankRepository repository;private BankController controller;private Worker worker;private PriceService prices;private boolean fail,visible=true;
	private Item[] contents=new Item[0],display=new Item[0];
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source.items=new Item[]{new Item(B,1)};prices=new PriceService(()->{Map<Integer,Long> q=new HashMap<>();q.put(N,100L);q.put(D,200L);return q;},()->SnapshotFixtures.TIME);prices.refresh();overlay.enable();launch();settle();controller.startRun(true);worker.runAll();bank(B,100,0,100000);}
	@After public void stop(){controller.stop();}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{view=v;overlay.updateBank(v);},prices);controller.start(()->{if(fail){throw new java.io.IOException("Synthetic interruption");}return root;},worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private void bank(int form,int nature,int bones,int coins){List<Item> items=new ArrayList<>();if(form>0){items.add(new Item(form,1));}if(nature>0){items.add(new Item(N,nature));}if(bones>0){items.add(new Item(D,bones));}items.add(new Item(C,coins));source.items=items.toArray(new Item[0]);source.observer.changed();settle();}
	private void proof(Item... items){contents=items;display=items.clone();source.bagSession=source.observe().getSession();source.bagAccess=new LootingBagReader.Access(){public boolean live(){return true;}public boolean visible(){return visible;}public Item[] contents(){return contents;}public Item[] displayed(){return display;}public String name(int id){return id==N?"Nature rune":id==D?"Dragon bones":"Coins";}};}
	private void buy(){BagObservation preview=source.bag();controller.buyBag(view.getRun().getRevision(),UUID.randomUUID(),preview,p->{});worker.runAll();}
	private BankRun saved() throws Exception{return repository.load(root,source.character).get();}
	@Test public void loadedBagCommitsOnePaymentAndAllOffsetsWithoutPhysicalChanges()throws Exception
	{proof(new Item(N,100),new Item(D,50),new Item(C,25000));BankRun old=saved();buy();assertEquals(0,saved().balance(N));assertEquals(-50,saved().balance(D));assertEquals(75000,saved().balance(C));assertEquals(65000,BuybackEngine.available(saved()));assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(old.getPhysical(),saved().getPhysical());assertEquals(old.getFinancial(),saved().getFinancial());assertEquals("1",overlay.getProjection().label(B));assertTrue(repository.history(root,saved(),null,false).getRows().stream().anyMatch(r->r.getCategory().equals("Automatic — Legacy Looting Bag")&&r.getPurchase()!=null));}
	@Test public void partialAndFullReturnsPreserveEarnedRunesAndNeverCreateGainOrPeak()throws Exception
	{proof(new Item(N,100),new Item(D,50),new Item(C,25000));long gain=saved().getFinancial().getGainedGp();Long peak=view.getValuation().getPeakGp();buy();bank(B,150,20,110000);assertEquals(50,saved().balance(N));assertEquals(-30,saved().balance(D));bank(B,200,50,125000);assertEquals(100,saved().balance(N));assertEquals(0,saved().balance(D));assertEquals(90000,BuybackEngine.available(saved()));assertEquals(gain,saved().getFinancial().getGainedGp());assertEquals(peak,view.getValuation().getPeakGp());}
	@Test public void emptyBagIsVerifiedOnlyWithCurrentVisibleMatchingServerContainer()throws Exception
	{proof();assertTrue(source.bag().fresh(Instant.now()));buy();assertTrue(saved().getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());assertEquals(90000,BuybackEngine.available(saved()));}
	@Test public void missingAndStalePacketsCannotSpendButFreshBankedDataNeedsNoInventoryPanel()throws Exception
	{buy();assertEquals(0,saved().getBuybacks().getCommittedGp());proof(new Item(N,100));source.bagSession=UUID.randomUUID();buy();assertEquals(0,saved().getBuybacks().getCommittedGp());source.bagSession=source.observe().getSession();visible=false;display=null;buy();assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(0,saved().balance(N));}
	@Test public void duplicateTypesAggregateAndCancellationPreviewDoesNotMutate()throws Exception
	{proof(new Item(D,1),new Item(D,1));BankRun old=saved();assertEquals(Long.valueOf(2),source.bag().getQuantities().get(D));controller.bagPreview(q->assertEquals(Long.valueOf(2),q.getQuantities().get(D)));assertEquals(old,saved());}
	@Test public void failedCommitAndChangedPreviewNeverPartiallySpendOrUnlock()throws Exception
	{proof(new Item(N,100));BankRun old=saved();BagObservation preview=source.bag();proof(new Item(N,101));controller.buyBag(old.getRevision(),UUID.randomUUID(),preview,p->fail("Unexpected acknowledgment"));worker.runAll();assertEquals(old,saved());proof(new Item(N,100));fail=true;buy();assertTrue(view.isError());assertEquals(old,saved());assertEquals(old,view.getRun());}
	@Test public void openClosedVariantsRestartAndRepeatPurchaseKeepOneEntitlementAndOffset()throws Exception
	{proof(new Item(N,500));buy();bank(O,100,0,100000);assertEquals("1",overlay.getProjection().label(O));assertEquals(-400,saved().balance(N));BankRun old=saved();controller.stop();source.observer.reset();launch();settle();proof(new Item(N,999));buy();assertEquals(old.getBuybacks().getPurchases(),saved().getBuybacks().getPurchases());assertEquals(-400,saved().balance(N));assertEquals(10000,saved().getBuybacks().getCommittedGp());}
	@Test public void naturallyEarnedOpenBagIsFreeAndDoesNotUnlockSeparateLegacyCopies()throws Exception
	{bank(O,100,0,100000);assertEquals("Available",BuybackEngine.status(saved(),BuybackCatalog.entry("looting_bag_family")));assertEquals("0",overlay.getProjection().label(O));bank(0,100,0,100000);bank(O,100,0,100000);assertEquals(0,BuybackEngine.balances(saved()).getOrDefault(O,0L).longValue());
	 source.items=new Item[]{new Item(B,1),new Item(O,1),new Item(N,100),new Item(C,100000)};source.observer.changed();settle();assertEquals(1,BuybackEngine.balances(saved()).values().stream().filter(n->n>0).count()-2);assertEquals(0,saved().getBuybacks().getCommittedGp());}
	@Test public void genuinelyEarnedBagNeedsNoPaymentAndUnavailableContentsCannotMasqueradeAsEmpty()
	{BankRun newRun=BankEngine.start(SnapshotFixtures.CHARACTER_B,null,new com.fromscratch.model.ContainerSnapshot(Collections.emptyMap(),0),true,UUID.randomUUID(),SnapshotFixtures.TIME);newRun=BankEngine.observe(newRun,new com.fromscratch.model.ContainerSnapshot(Collections.singletonMap(O,1L),1),UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals("Earned",BuybackEngine.status(newRun,BuybackCatalog.entry("looting_bag_family")));assertEquals(Long.valueOf(1),BuybackEngine.balances(newRun).get(O));proof();contents=null;assertFalse(source.bag().fresh(Instant.now()));}
	@Test public void bothBankedEmptyFormsWorkWithoutContentsWidgetsAndShowEmptyConfirmation()throws Exception
	{for(int form:new int[]{B,O}){bank(form,100,0,100000);proof();visible=false;display=null;BagObservation observed=source.bag();assertTrue(observed.fresh(Instant.now()));assertEquals(form,observed.getItemId());assertTrue(observed.getQuantities().isEmpty());String text=BuybackDialog.bagConfirmation(view.getRun(),observed);assertTrue(text.contains("Your Looting Bag is empty. No legacy item adjustments are necessary."));assertFalse(text.contains("negative virtual"));}}
	@Test public void nativeInspectionAcceptsEmptyTextOrMatchingFreshPacketAndIgnoresDecorativeChild()
	{LootingBagReader.Access nativeView=new LootingBagReader.Access(){public boolean live(){return true;}public boolean visible(){return true;}public Item[] contents(){return contents;}public Item[] displayed(){return display;}public String name(int id){return "Nature rune";}public boolean emptyMessage(){return true;}};LootingBagReader reader=new LootingBagReader(nativeView);contents=null;display=new Item[]{new Item(-1,0)};assertTrue(reader.inspect(source.observe(),B,false).fresh(Instant.now()));contents=new Item[]{new Item(N,500)};display=new Item[]{new Item(N,500),new Item(-1,0)};assertFalse(reader.inspect(source.observe(),O,false).fresh(Instant.now()));assertEquals(Long.valueOf(500),reader.inspect(source.observe(),O,true).getQuantities().get(N));display=new Item[]{new Item(N,499)};assertFalse(reader.inspect(source.observe(),O,true).fresh(Instant.now()));}
	@Test public void filteredOutLegacyBagStillQualifiesAndOnlyCommittedPurchaseChangesMembership()throws Exception
	{
		final net.runelite.client.plugins.banktags.BankTag[] tag={null};VirtualBankMode filter=new VirtualBankMode(new VirtualBankMode.Access(){String active;public void register(String name,net.runelite.client.plugins.banktags.BankTag value){tag[0]=value;}public void unregister(String name){tag[0]=null;}public void open(String name){active=name;}public void close(){active=null;}public String active(){return active;}public void refresh(){}});filter.setEnabled(true);controller.informationActions(r->filter.update(r,source.open));controller.tick();worker.runAll();assertFalse(tag[0].contains(B));assertEquals(1,saved().physical(B));proof(new Item(N,100));controller.buyBag(view.getRun().getRevision(),UUID.randomUUID(),source.bag(),p->{});assertFalse(tag[0].contains(B));worker.runAll();assertTrue(tag[0].contains(B));assertFalse(tag[0].contains(N));assertEquals(1,saved().physical(B));filter.stop();
	}
}
