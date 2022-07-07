import Meddra.*;
import Handlers.*;
import Meddra.MeddraFreq;
import Utils.Utils;
import Utils.UtilsFonctions;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import Utils.UtilsFonctions;

public class MainGR {

    public static void main(String[] args) throws Exception {
//        Analyzer analyzer = new StandardAnalyzer();
        String sideEffect="ocular/metastatic";
        ArrayList<ArrayList<String>> indicationResult= UtilsFonctions.AllMedraWhichHealSe(sideEffect);
        UtilsFonctions.printResult(indicationResult);
        ArrayList<ArrayList<String>> freqResult=UtilsFonctions.AllMedraWhichCauseSe(sideEffect);
        UtilsFonctions.printResult(freqResult);
//        System.out.println("Frequence Se -> tot= "+ MeddraFreq.numberRequest +", lostChem= "+MeddraFreq.lostRequestByChemical+", lostATC= "+MeddraFreq.lostRequestByATC);
//        System.out.println("Indication   -> tot= "+ MeddraIndications.numberRequest +", lostChem= "+MeddraIndications.lostRequestByChemical+", lostATC= "+MeddraIndications.lostRequestByATC);
//        List<Document> docs = DrugBankHandler.searchDrugBySideEffect("pain",analyzer,DrugBankHandler.loadIndex(),100000);
//        for (Document doc:docs){
//            System.out.println(doc.get("id")+doc.get("Toxicity")+doc.get("Indication")+doc.get("Pharmacology"));
//        }
//        docs = DrugBankHandler.searchDrugBySideEffect("pain",analyzer,DrugBankHandler.loadIndex(),100000);
//        for (Document doc:docs){
//            System.out.println(doc.get("id")+doc.get("Toxicity")+doc.get("Indication")+doc.get("Pharmacology"));
//        }
//        docs = OmimOntoHandler.searchDrugByClassId("HP:0000175",analyzer,OmimOntoHandler.loadIndex(),10000);
//        for (Document doc:docs){
//            System.out.println(doc);
//        }
//        System.out.println(UtilsFonctions.stringTreatmentStar("Hypoplasia of the uterus"));
//        System.out.println(UtilsFonctions.stringTreatmentSe("Hypoplasia of the uterus"));
//        System.out.println(UtilsFonctions.stringTreatmentIndication("Hypoplasia of the uterus"));
        System.out.println(UtilsFonctions.avoidError("adsjks_jksqkj/jdsqds&dsqn"));
        System.out.println(UtilsFonctions.avoidError("adsjks/jksqkj/jdsqds&dsqn"));
        System.out.println(UtilsFonctions.avoidError("adsjks&jksqkj/jdsqds&dsqn"));
    }

}
