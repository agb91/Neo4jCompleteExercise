package Twins;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import global.Globals;
import talkToDb.ORM;
import talkToDb.Tripletta;
import usefullAbstract.GenericGraphHandler;

public class GeneralBadTwin extends GenericGraphHandler{
	
	static Vector<String> Tprimo = new Vector<String>();
	
	public static void createBadTwinGeneral(int livello)
	{
		long prima = System.currentTimeMillis();
		long dbTime = 0;
		if(!inInteger(livello,Globals.badTwinDid))
		{
			try ( Transaction tx = Globals.graphDb.beginTx() )
			{
				HashMap<String,Relationship> allRelationsUntilNow = getAllRelationsUntilHash(livello, Globals.allRelationsGeneralHash);
				//System.out.println("aRUN size :  " + allRelationsUntilNow.size());
				Vector<String> tPrimo = new Vector<String>();
				tPrimo = riempiTPrimo(livello);
				
				for(int i=1; i<Globals.allNodes.size(); i++)
				{
					Node nodoAttuale = Globals.allNodes.get(i);
					String nomeNodo = pulisci(nodoAttuale.getProperties("name").values().toString());
					Iterator<String> keyset = allRelationsUntilNow.keySet().iterator();
					while(keyset.hasNext())
					{ 
						String a = keyset.next();
						Relationship transazioneAttuale = allRelationsUntilNow.get(a);
						String nomeTransizione = pulisci(transazioneAttuale.getProperties("type").values().toString());					
						//System.out.println("nome transizione attuale:   " + nomeTransizione);
						String from = pulisci(transazioneAttuale.getProperties("from").values().toString());
						if(from.equalsIgnoreCase("inizio"))
						{
							continue;
						}
						String osservabile = pulisci(transazioneAttuale.getProperties("oss").values().toString());
						Node destinazione = transazioneAttuale.getEndNode();
						boolean bool = from.equalsIgnoreCase(nomeNodo) 
								&& osservabile.equalsIgnoreCase("y");
						//System.out.println("nome nodo: " + nomeNodo + ";   from : " + from + ";  bool : "+ bool);
						if(bool)
						{
							String eventoTransizione = pulisci(transazioneAttuale.getProperties("event").values().toString());
							int cardinalita = getCardinalita(eventoTransizione);
							boolean fault;
							String guasto = pulisci(transazioneAttuale.getProperties("guasto").values().toString());
							if(guasto.equalsIgnoreCase("y"))
							{
								//System.out.println("ho trovato guasto");
								fault = true;
							}
							else
							{
								fault = false;
							}
							//System.out.println("fauls:  " + fault + ";     guasto: " + guasto);
							// il grafo A è già definito globalmente
							HashMap<String,Tripletta> insiemeTriplette = Find.findHash(destinazione,(livello-cardinalita),fault,eventoTransizione, livello-1); 
							Iterator<String> ks = insiemeTriplette.keySet().iterator();
							while(ks.hasNext())
							{ 
								String an = ks.next();
								Tripletta triplettaAttuale = insiemeTriplette.get(an);
								//System.out.println(triplettaAttuale.getEventoOrdered() + "---" + triplettaAttuale.getsDestinazione());
								String eventoTripletta = triplettaAttuale.getEvento();
								String guastoAttuale = "n";
								if(triplettaAttuale.isFaultPrimo())
								{
									guastoAttuale = "y";
								}							
								if(!identici(eventoTripletta))
								{
									String sorgente = pulisci(nodoAttuale.getProperties("name").values().toString());
									String destination = pulisci(triplettaAttuale.getsDestinazione().getProperties("name").values().toString());
									String id = sorgente + "--" + triplettaAttuale.getEventoOrdered() + "--" + destination + "--" + guastoAttuale;
									//System.out.println("tripletta: " + id);
									long before = System.currentTimeMillis();
									addRelationBad(nodoAttuale, triplettaAttuale.getsDestinazione(), 
											id, "y", triplettaAttuale.getEvento() , guastoAttuale, livello);
									long after = System.currentTimeMillis();
									dbTime += (after-before);
									tPrimo.add(id);
								}
							}
						}
					}
				}		
				long dopo = System.currentTimeMillis();
				System.err.println("tempo per bad twin : " + ((dopo-prima)- dbTime) );
				//ORM.updateDb(livello);
				//remove IsolatedStatesBad(livello);
				System.out.println("---------------------------------------------");
				System.out.println("created bad twin level" + livello);
				tx.success();
				Globals.badTwinDid.addElement(Integer.valueOf(livello));
			}
		}
	}
		
	
	/*public static boolean checkC2C3(int level)
	{
		//secondo caso se è deterministico allora è diagnosticabile
		if(deterministic(Globals.allRelationsGeneral.get(level)))
		{
			System.out.println("vale C2: il bad twin è deterministico");
			return true;
		}
		else
		{
			System.out.println("non vale C2, il bad twin è non deterministico");
		}
		
		//cerco le transizioni di guasto: prendo il loro evento.
		// se per tutti quegli eventi non esistono transizioni di guasto
		// che abbiano come evento quegli eventi allora è diagnosticabile
		Vector<Relationship> allBadTwinLocal = 
				getAllRelationsUntil(level, Globals.allRelationsGeneral);
		if(thirdCondition(allBadTwinLocal))
		{
			System.out.println("vale la C3");
			return true;
		}
		else
		{
			System.out.println("non vale la C3");
		}
		return false;
	}*/
	
	private static boolean identici( String evento)
	{
		String elenco[] = evento.split("//");
		for(int a=0; a<elenco.length ; a++)
		{
			for(int k=0; k<elenco.length; k++)
			{
				if(a!=k)
				{
					if(!elenco[k].equalsIgnoreCase(elenco[a]))
					{
						//System.out.println("entra in identici: " + evento + "rispondo false;");
						return false;
					}
				}
			}
		}
		//System.out.println("entra in identici: " + evento + "rispondo true;");
		return true;		
	}
	
	private static int getCardinalita(String tr)
	{
		int ris = 0;
		ris = tr.split("//").length;
		return ris;
	}
}