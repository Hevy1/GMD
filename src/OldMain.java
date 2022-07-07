import Handlers.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;

/** Index all text files under a directory. */
public class OldMain {

    private static Analyzer analyzer;
    private static Directory idx;

    /** Index all lines of a text file (path of the file is args[0]). */
    public static void main(String[] args) {
        analyzer = new StandardAnalyzer();

        idx = DrugBankHandler.loadIndex();
        searchDrug("l1055");


        //createAllIndex();

        /*idx = ChemicalSourcesHandler.loadIndex();
        if (idx != null) {
            try {
                ArrayList<Document> docs = ChemicalSourcesHandler.searchAll(null, idx, 100);
                for (Document doc : docs) {
                    System.out.println(doc.get("atc"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    public static void searchDrug(String toxicity){
        try {
            int hits = 1000;
            ArrayList<Document> docs =  DrugBankHandler.searchDrugByIndication(toxicity, analyzer, idx, hits );
            DrugBankHandler.printDocuments(docs);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }


    public static void searchIndication(String name){
        name = name.toLowerCase();

        try {
            int hits = 1000;
            ArrayList<Document> docs =  MeddraIndicationsHandler.searchIndicationByLabel(name, analyzer, idx, hits );
            MeddraIndicationsHandler.printDocuments(docs);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static void searchSideEffect(String name) {
        name = name.toLowerCase();

        try {
            int hits = 1000;
            ArrayList<Document> docs = MeddraHandler.searchSideEffectByLabel(name, analyzer, idx, hits);
            MeddraHandler.printDocuments(docs);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static void createAllIndex() {
        ATCHandler.createIndex(analyzer);
        ChemicalSourcesHandler.createIndex(analyzer);
        DrugBankHandler.createIndex(analyzer);
        HPOHandler.createIndex(analyzer);
        MeddraAllSE.createIndex(analyzer);
        MeddraFreq.createIndex(analyzer);
        MeddraHandler.createIndex(analyzer);
        MeddraIndicationsHandler.createIndex(analyzer);
        OmimHandler.createIndex(analyzer);
        OmimOntoHandler.createIndex(analyzer);
    }
}