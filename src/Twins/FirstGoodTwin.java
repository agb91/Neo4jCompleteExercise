package Twins;

import java.util.Iterator;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import global.Globals;
import talkToDb.ORM;
import talkToDb.ORM.RelTypes;
import usefullAbstract.GenericGraphHandler;

public class FirstGoodTwin extends GenericGraphHandler{
	
	public static void createGoodTwinLevel1()
	{
		goodBase();
		removeGuasti();
		removeIsolatedStatesGood();
		System.out.println("created good twin level 1");
	}
	
	//crea il grafo good iniziale
	private static void goodBase()
	{
		try ( Transaction tx1 = Globals.graphDb.beginTx() )
		{
			Globals.allNodesGood.clear();
			Globals.allRelationsGood.clear();
			
			for(int i=0; i<Globals.allNodes.size(); i++)
			{
				Node attuale = Globals.allNodes.get(i);
				String nome = attuale.getProperty("name").toString();
				addNodeGood(nome);
			}
			
			for(int i=0; i<Globals.allRelations.size(); i++)
			{
				Relationship attuale = Globals.allRelations.get(i);
				String nome = attuale.getProperties("type").values().toString();
				String n1 = attuale.getStartNode().getProperties("name").values().toString();
				String n2 = attuale.getEndNode().getProperties("name").values().toString();
				String oss = attuale.getProperties("oss").values().toString();
				String ev = attuale.getProperties("event").values().toString();
				String gu = attuale.getProperties("guasto").values().toString();
				addRelationGood(n1, n2, nome, oss, ev, gu);
			}
			
			tx1.success();
		}	
		
	}
	
	private static void addRelationGood(String n1, String n2, String nome, String oss, String ev, String gu) {
		Relationship relationship = null;
		try ( Transaction tx = Globals.graphDbGood.beginTx() )
		{
			Node n1Node = findNodeByName(pulisci(n1));
			Node n2Node = findNodeByName(pulisci(n2));
			relationship = n1Node.createRelationshipTo( n2Node, RelTypes.STD );
			relationship.setProperty( "type", pulisci(nome) );
			relationship.setProperty( "oss", pulisci(oss) );
			ev = pulisci(ev);
			relationship.setProperty("event", pulisci(ev));
			relationship.setProperty("guasto", pulisci(gu));
			relationship.setProperty("from", pulisci(n1));
			relationship.setProperty("to", pulisci(n2));
			tx.success();
			Globals.allRelationsGood.addElement(relationship);
			//System.out.println("ho aggiunto la relazione: " + nome + "  da: " + nomeN1 + "  a: " + nomeN2);
		}	
	}

	private static Node findNodeByName(String n2) {
		n2 = pulisci(n2);
		Node returned = null;
		try ( Transaction tx = Globals.graphDbGood.beginTx() )
		{
			for(int i=0; i<Globals.allNodesGood.size(); i++)
			{
				Node attuale = Globals.allNodesGood.get(i);
				String nomeAttuale = attuale.getProperties("name").values().toString();
				nomeAttuale = pulisci(nomeAttuale);
				/*System.out.println("attuale: " + nomeAttuale);
				System.out.println("n2 : " + n2);
				System.out.println("------------------------------------------------------");
				*/
				if(n2.equalsIgnoreCase(nomeAttuale))
				{
					returned = attuale;
				}
			}
			tx.success();
		}
		return returned;
	}

	private static void addNodeGood(String n) {
		Node userNode = null;
		try ( Transaction tx = Globals.graphDbGood.beginTx() )
		{
		    Label label = DynamicLabel.label( "Nome" );
	        userNode = Globals.graphDbGood.createNode( label );
	        userNode.setProperty( "name", n);
	        Globals.allNodesGood.addElement(userNode);
		    tx.success();
		}    		
	}

	private static void removeGuasti()
	{
		try ( Transaction tx = Globals.graphDbGood.beginTx() )
		{
			for(int i=0; i<Globals.allRelationsGood.size(); i++)
			{
				Relationship attuale = Globals.allRelationsGood.get(i);
				String guasto = attuale.getProperties("guasto").values().toString();
				System.out.println("vedo guasto: " + guasto);
				if(guasto.toLowerCase().contains("y"))
				{
					System.out.println("e me ne libero: " + Globals.allRelationsGood.get(i).getId());
					
					Globals.allRelationsGood.get(i).delete();
					Globals.allRelationsGood.remove(i);
					i--;
				}
			}
			tx.success();
		}	
	}
	
	public static void removeIsolatedStatesGood()
	{
		try ( Transaction tx1 = Globals.graphDbGood.beginTx() )
		{
			boolean raggiungibile = false;
			for(int i=1; i<Globals.allNodesGood.size(); i++)
			{
				raggiungibile = checkPathFromRootGood(Globals.allNodesGood.get(i));
				if(!raggiungibile)
				{
					killNode(Globals.allNodesGood.get(i), i);
					i--;
				}
			}
			tx1.success();
		}	
	}
	
	//prima elimino tutte le relazioni che partono da n
	//poi elimino n
	public static void killNode(Node n, int index)
	{
		Globals.allNodesGood.remove(index);
		String nomeNode = n.getProperties("name").values().toString();
		for(int a=0; a<Globals.allRelationsGood.size(); a++)
		{
			Relationship r = Globals.allRelationsGood.get(a);
			String fromr = r.getProperties("from").values().toString();
			if(fromr.contains(nomeNode))
			{
				Globals.allRelationsGood.get(a).delete();
				Globals.allRelationsGood.remove(a);
				a--;		
			}
		}
		n.delete();
	}
	

	public static boolean checkPathFromRootGood(Node n)
	{
		boolean raggiungibile = false;
		
		Node root = Globals.allNodesGood.get(0);
		Iterator<Path> tuttiIPath = findPath(root,n);
		while(tuttiIPath.hasNext() && !raggiungibile)
		{
			Path path = tuttiIPath.next();
			if(path.relationships().iterator().hasNext()) 
			{
				raggiungibile = true;
			}
		}
		return raggiungibile;
	}

}