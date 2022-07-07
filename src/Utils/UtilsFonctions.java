package Utils;

import Handlers.ATCHandler;
import Handlers.ChemicalSourcesHandler;
import Handlers.DrugBankHandler;
import Handlers.OmimHandler;
import Handlers.HPOHandler;
import Meddra.MeddraFreq;
import Meddra.MeddraIndications;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UtilsFonctions {

    //should be use to show tab result properly
    public static void printResult(ArrayList<ArrayList<String>> result){
        for (ArrayList<String> ligne:result) {
            System.out.print("| ");
            for (int i=0;i<ligne.size();i++){
                int maxtaille=15;
                if(i==0){
                    maxtaille=20;
                }
                else if(i==1){
                    maxtaille=40;
                }
                else if(i==2){
                    maxtaille=40;
                }
                int taille=ligne.get(i).length();
                StringBuilder sb=new StringBuilder();
                sb.append(ligne.get(i));
                while (taille<maxtaille){
                    sb.append(" ");
                    taille++;
                }
                System.out.print(sb.append(" | "));
            }
            System.out.println();
        }
    }

    //clear our tab retiring blank
    //use to clear correspondance between table
    public static ArrayList<ArrayList<String>> Clear(ArrayList<ArrayList<String>> tab,int j){
        ArrayList<ArrayList<String>> newTab =new ArrayList<ArrayList<String>>();
        for(int i=0;i<tab.size();i++){
            if(!tab.get(i).get(j).equals("")){
                newTab.add(tab.get(i));
            }
        }

        return newTab;
    }

    //make the correspondance between ID string and ATC string using chemicalsources
    public static ArrayList<ArrayList<String>> ChangeIDByATC(ArrayList<ArrayList<String>> tab,int j) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        Directory idx = ChemicalSourcesHandler.loadIndex();
        ArrayList<Document> documents= new ArrayList<Document>();
        for (ArrayList<String> ligne : tab) {
            documents = ChemicalSourcesHandler.searchDrugByChemical(ligne.get(j), analyzer, idx, 10000);
            if(documents.isEmpty()){
                ligne.set(j,"");
            }
            else{
                String newID=ChemicalSourcesHandler.getFirstDocuments(documents);
                ligne.set(j,newID);
            }

        }
        return tab;
    }

    //make the correspondance between ATC string and name using .keg
    public static ArrayList<ArrayList<String>> ChangeATCByName(ArrayList<ArrayList<String>> tab,int i) throws IOException, ParseException {
        Analyzer analyzer= new StandardAnalyzer();
        List<Document> docs= new ArrayList<Document>();
        for (ArrayList<String> ligne : tab) {
            //System.out.println(ligne.get(0));
            docs = ATCHandler.searchNameById(ligne.get(i),10000);

            if (docs.isEmpty()){
                ligne.set(i,"");
            }
            else{
                String newID=docs.get(0).get("Name_Drug");
                ligne.set(i,newID);
            }
        }
        return tab;
    }

    //return the mapping indication for indication
    public static float MappingQualityIndication(){
        if(MeddraIndications.numberRequest==0){
            return 100;
        }
        return ((MeddraIndications.numberRequest-MeddraIndications.lostRequestByChemical-MeddraIndications.lostRequestByATC)*1000/MeddraIndications.numberRequest)/10;
    }
    //return the mapping indication for se
    public static float MappingQualitySeFreq(){
        if(MeddraFreq.numberRequest==0){
            return 100;
        }
        return ((MeddraFreq.numberRequest-MeddraFreq.lostRequestByChemical-MeddraFreq.lostRequestByATC)*1000/MeddraFreq.numberRequest)/10;
    }

    //using a side effect and return a tab of Meddra which can cause the effect
    // attention here we use only SQL data
    public static ArrayList<ArrayList<String>> giveMeddraWhichCauseSe(String sideEffect) throws Exception {
        MeddraFreq mf=new MeddraFreq();
        ArrayList<ArrayList<String>> tab=mf.getMeddra(stringTreatmentSe(sideEffect));
        MeddraFreq.numberRequest=tab.size();
        tab=ChangeIDByATC(tab,0);
        tab=Clear(tab,0);
        tab=ChangeIDByATC(tab,1);
        tab=Clear(tab,1);
        MeddraFreq.lostRequestByChemical=MeddraFreq.numberRequest-tab.size();
        tab=ChangeATCByName(tab,0);
        tab=Clear(tab,0);
        tab=ChangeATCByName(tab,1);
        tab=Clear(tab,1);
        MeddraFreq.lostRequestByATC=MeddraFreq.numberRequest-tab.size()-MeddraFreq.lostRequestByChemical;
        MeddraFreq.mappingPrecision=MappingQualitySeFreq();
        return tab;
    }

    //using a side effect and return a tab of Meddra which can cause the effect
    // attention here we use only SQL data
    public static ArrayList<ArrayList<String>> giveMeddraWhichHealSe(String sideEffect) throws Exception {
        MeddraIndications mi=new MeddraIndications();
        ArrayList<ArrayList<String>> tab=mi.getIndication(stringTreatmentIndication(sideEffect));
        MeddraIndications.numberRequest=tab.size();
        tab=ChangeIDByATC(tab,0);
        tab=Clear(tab,0);
        MeddraIndications.lostRequestByChemical=MeddraIndications.numberRequest-tab.size();
        tab=ChangeATCByName(tab,0);
        tab=Clear(tab,0);
        MeddraIndications.lostRequestByATC=MeddraIndications.numberRequest-tab.size()-MeddraIndications.lostRequestByChemical;
        MeddraFreq.mappingPrecision=MappingQualitySeFreq();
        return tab;
    }

    //treat the string to prepare it for the use of _
    public static String stringTreatmentStar(String sideEffect) throws Exception {
        Analyzer analyzer= new StandardAnalyzer();
        String newSideEffect=sideEffect;
        if(sideEffect.contains("_")){
            newSideEffect="";
            int count=1;
            for (String str: sideEffect.split("_")) {
                if (str.equals("")){
                    newSideEffect=newSideEffect+str;
                    count++;
                }
                else if (count==(sideEffect.split("_")).length){
                    newSideEffect=newSideEffect+str;
                }
                else{
                    newSideEffect=newSideEffect+str+"%";
                    count++;
                }
            }
        }
        //gestion of synonyms
        else{
            List<String> strings = HPOHandler.getSynonymsByName(sideEffect,analyzer,HPOHandler.loadIndex());
            for(String str:strings){
                if(str!=""){
                    newSideEffect=newSideEffect+"/"+str;
                }
            }
        }
        return newSideEffect;
    }

    //using a side effect and return a tab of Meddra which can heal the effect
    public static ArrayList<ArrayList<String>> AllMedraWhichHealSe(String sideEffect) throws Exception {

        ArrayList<ArrayList<String>> allMeddra = giveMeddraWhichHealSe(sideEffect);
        System.out.println(avoidError(sideEffect));
        ArrayList<ArrayList<String>> all=updateIndicationTabWithDrugbank(allMeddra,avoidError(sideEffect));
        MeddraIndications.numberRequest=all.size();
        return all;
    }

    //using a side effect and return a tab of Meddra which can cause the effect
    //it uses freq
    public static ArrayList<ArrayList<String>> AllMedraWhichCauseSe(String sideEffect) throws Exception {

        ArrayList<ArrayList<String>> allMeddra = giveMeddraWhichCauseSe(sideEffect);
        System.out.println(avoidError(sideEffect));
        ArrayList<ArrayList<String>> all=updateSideEffectTabWithDrugbank(allMeddra,avoidError(sideEffect));
        MeddraFreq.numberRequest=all.size();
        return all;
    }

    //parse the String using "/"
    public static List<String> stringTreatmentOr(String sideEffect){
        String newSideEffect="";
        List<String> listSE= new ArrayList<String>();
        if(sideEffect.contains("/")){

            int count=1;
            for (String str: sideEffect.split("/")) {
                if (str!=""){
                    listSE.add(str);
                }
            }
        }
        return listSE;
    }

    //parse the String using "&"
    public static List<String> stringTreatmentAnd(String sideEffect){
        String newSideEffect="";
        List<String> listSE= new ArrayList<String>();
        if(sideEffect.contains("&")){

            int count=1;
            for (String str: sideEffect.split("&")) {
                if (str!=""){
                    listSE.add(str);
                }
            }
        }
        return listSE;
    }

    //construct the OR research for SQL data
    public static String queryTreatementOr(List<String> listSE, String tableName){
        String query="";
        for (String str : listSE){
            if (query.equals("")){
                query=query+tableName+" LIKE '%" + str +"%'";
            }
            else {
                if(str.charAt(str.length()-1) == ' ')
                    str = str.substring(0,str.length()-1);
                if(str.charAt(0) == ' ')
                    str = str.substring(1);
                query=query+ " OR "+tableName+" LIKE '%" + str +"%'";
            }

        }
        return query;
    }

    //make the string for And research in SQL data
    public static String queryTreatementAnd(List<String> listSE, String tableName){
        String query="";
        for (String str : listSE){
            if (query.equals("")){
                query=query+tableName+" LIKE '%" + str +"%'";
            }
            else {
                if(str.charAt(str.length()-1) == ' ')
                    str = str.substring(0,str.length()-1);
                if(str.charAt(0) == ' ')
                    str = str.substring(1);
                query=query+ " AND "+tableName+" LIKE '%" + str +"%'";
            }

        }
        return query;
    }
    //treat the String to make it good for our query using sideEffect
    public static String stringTreatmentSe(String sideEffect) throws Exception {
        String queryParam="";
        String temp=queryParam+stringTreatmentStar(sideEffect);
        if (temp.contains("&")) {
                queryParam = queryTreatementAnd(stringTreatmentAnd(temp), "`side_effect_name`");
            }
        else if (temp.contains("/")) {
            queryParam = queryTreatementOr(stringTreatmentOr(temp), "`side_effect_name`");
        }
        else{
            queryParam="`side_effect_name` LIKE '%"+temp+"%'";
        }
        return queryParam;
    }

    //treat the String to make it good for our query using Indication
    public static String stringTreatmentIndication(String sideEffect) throws Exception {
        String queryParam1="";
        String queryParam2="";
        String temp1=queryParam1+stringTreatmentStar(sideEffect);
        String temp2=queryParam1+stringTreatmentStar(sideEffect);
        if (temp1.contains("&")){
            queryParam1=queryTreatementAnd(stringTreatmentAnd(temp1),"`concept_name`");
        }
        else if(temp1.contains("/")){
            queryParam1=queryTreatementOr(stringTreatmentOr(temp1),"`concept_name`");
        }
        else{
            queryParam1="`concept_name` LIKE '%"+temp1+"%'";
        }

        if (temp2.contains("&")){
            queryParam2=queryTreatementAnd(stringTreatmentAnd(temp2),"`meddra_concept_name`");
        }
        else if(temp1.contains("/")){
            queryParam2=queryTreatementOr(stringTreatmentOr(temp2),"`meddra_concept_name`");
        }
        else{
            queryParam2="`meddra_concept_name` LIKE '%"+temp2+"%'";
        }
        return queryParam1+" OR "+queryParam2;
    }

    /*public static ArrayList<ArrayList<String>> updateTable(ArrayList<ArrayList<String>> tab, List<Document> docs, int sourceColomn){
        for (Document doc:docs){
            Boolean bool=false;
            for (ArrayList<String> ligne:tab){
                if(doc.get("name").equals(ligne.get(0))){
                    bool=true;
                    //ajouter seulement la source à la liste des sources
                    ligne.set(sourceColomn,ligne.get(sourceColomn)+", "+"DrugBank");
                }
            }
            if (bool=false){
                ArrayList<String> docToList= new ArrayList<String >();
                //à adapter
                docToList.add(doc.get("name"));
                docToList.add(doc.get("Toxicity"));
                docToList.add(doc.get("Pharmacology"));
                docToList.add(doc.get("Indication"));
                docToList.add("no Indication"); //pour les colonnes sans correspondance
                tab.add(docToList);
            }
            return tab;
        }
        return tab;
    }*/

    //it makes the correspondance between SQL and Index research and using a tab and a sideEffect
    public static ArrayList<ArrayList<String>> updateIndicationTabWithDrugbank(ArrayList<ArrayList<String>> currentData, String sideEffect) throws IOException, ParseException {

        Analyzer analyzer = new StandardAnalyzer();
        Directory idx = DrugBankHandler.loadIndex();

        int hits = 100000;
        ArrayList<Document> docs =  DrugBankHandler.searchDrugByIndication(sideEffect, analyzer, idx, hits );

        for (Document doc:docs){
            Boolean bool=false;
            for (ArrayList<String> ligne:currentData){
                if(doc.get("name").equals(ligne.get(0))){
                    bool=true;
                    //ajouter seulement la source à la liste des sources
                    ligne.set(3,ligne.get(3)+", "+"DrugBank");
                }
            }
            if (bool==false){
                ArrayList<String> docToList= new ArrayList<String >();
                //à adapter
                docToList.add(doc.get("name"));
                if(doc.get("Toxicity").contains(sideEffect)) {
                    if (doc.get("Pharmacology").contains(sideEffect)){
                        docToList.add(doc.get("Toxicity"));
                        docToList.add(doc.get("Pharmacology"));
                    }else{
                        docToList.add(doc.get("Toxicity"));
                        docToList.add("");
                    }
                }
                else{
                    docToList.add(doc.get("Pharmacology"));
                    docToList.add("");
                }
                docToList.add("DrugBank");
                currentData.add(docToList);
            }
        }
        return currentData;
    }

    //it makes the correspondance between SQL and Index research and using a tab and a sideEffect
    public static ArrayList<ArrayList<String>> updateSideEffectTabWithDrugbank(ArrayList<ArrayList<String>> currentData, String sideEffect) throws IOException, ParseException {

        Analyzer analyzer = new StandardAnalyzer();
        Directory idx = DrugBankHandler.loadIndex();

        int hits = 100000;
        ArrayList<Document> docs =  DrugBankHandler.searchDrugBySideEffect(sideEffect, analyzer, idx, hits );

        for (Document doc:docs){
            Boolean bool=false;
            for (ArrayList<String> ligne:currentData){
                if(doc.get("name").equals(ligne.get(0))){
                    bool=true;
                    //ajouter seulement la source à la liste des sources
                    ligne.set(4,ligne.get(4)+", "+"DrugBank");
                }
            }
            if (bool==false){
                ArrayList<String> docToList= new ArrayList<String >();
                //à adapter
                docToList.add(doc.get("name"));
                docToList.add("");
                docToList.add(doc.get("Indication"));
                docToList.add("Unknown");
                docToList.add("DrugBank");
                currentData.add(docToList);
            }
        }
        return currentData;
    }

    public static List<List<String>> getDisease(String effect) throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        Directory idx = OmimHandler.loadIndex();
        return OmimHandler.searchSymptomAND(effect, analyzer, idx, 10000);
    }

    // to avoid an error on Drugbank research using " "
    public static String avoidError(String se){
        if(se.contains("&")) {
            String[] ListSe = se.split("&");
            se=ListSe[0];
        }
        if(se.contains("/")){
            String[] ListSe2=se.split("/");
            se=ListSe2[0];
        }
        if(se.contains("_")){
            String[] ListSe3=se.split("_");
            se=ListSe3[0];
        }
        if(se.charAt(se.length()-1) == ' ')
            se = se.substring(0,se.length()-1);

        return se;
    }
}
