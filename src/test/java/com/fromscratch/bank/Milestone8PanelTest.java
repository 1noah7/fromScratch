package com.fromscratch.bank;

import com.fromscratch.playtime.PlaytimeTracker;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Milestone8PanelTest
{
	@Test public void measuredTimeAndConfigurationOnlyVirtualViewPreserveTwoColumnsAndSectionOrder() throws Exception
	{
		SwingUtilities.invokeAndWait(()->
		{
			BankPanel panel=new BankPanel();BankRun run=BankFixtures.run(1);
			BankActions actions=new BankActions()
			{
				public void startRun(boolean attested){} public void anchor(UUID r,com.fromscratch.model.ContainerSnapshot b,boolean a){} public void correct(UUID r,int id,long n){}public void reset(UUID r){}public void retry(){}
			};
			panel.render(new BankView(run,null,Collections.emptyMap(),false,false,"Bank progress updates automatically."),actions);
			panel.playtime(new PlaytimeTracker.View(run.getCharacter(),run.getRunId(),9000,null));
			java.util.List<String> labels=new ArrayList<>();visit(panel,labels);
			assertTrue(labels.contains("1 hour"));assertFalse(labels.stream().anyMatch(t->t.contains("Active Run")));
			String[] order={"From Scratch","Legacy Buybacks","Played Elsewhere?","Grand Exchange History","Bank History","Reset Progress"};int previous=-1;
			for(String heading:order) { int index=labels.indexOf(heading);assertTrue(heading,index>previous);previous=index; }
		});
	}
	private static void visit(Container parent,java.util.List<String> text)
	{
		if("statisticTiles".equals(parent.getName())) { assertEquals(2,((GridLayout)parent.getLayout()).getColumns());assertEquals(6,parent.getComponentCount()); }
		for(Component c:parent.getComponents())
		{
			if(c instanceof JLabel) { text.add(((JLabel)c).getText()); }
			assertNotEquals("virtualBankAction",c.getName());
			if(c instanceof Container) { visit((Container)c,text); }
		}
	}
}
