package Handlers;

import Utils.Utils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OmimHandler {

    static final String INDEX_DIR_PATH = "indexes/omim_index";
    static final String FILE_PATH = "data/OMIM/omim.txt";

    public static List<Document> searchDiseaseBySymptom(String symptom, Analyzer analyzer, Directory index, int hits) throws Exception {
        // Usual search request with IndexReader and searchers
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("Body_Parts", analyzer).parse('"' + symptom + '"');
        TopDocs top = indexSearcher.search(q1, hits);
        ArrayList<Document> docs = new ArrayList<>();
        for (ScoreDoc docu : top.scoreDocs) {
            docs.add(indexSearcher.doc(docu.doc));
        }
        return docs;
    }

    /**
     *
     * @param symptom : symptom name
     * @param analyzer : Lucene analyzer
     * @param index : Lucene index, needs to be omim_index
     * @param hits : number of documents returned
     * @return List of content of a document, stocked as a list of string (disease name, "Omim", score, symptoms)
     */
    public static List<List<String>> parsedSearchDBS(String symptom, Analyzer analyzer, Directory index, int hits) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new ComplexPhraseQueryParser("Body_Parts", analyzer).parse('"' + symptom + '"');
        //Query q1 = new QueryParser("Body_Parts", analyzer).parse('"' + symptom + '"');
        TopDocs top = indexSearcher.search(q1, hits);

        List<List<String>> docs = new ArrayList<>();
        // For each document, we will add the information we need in the proper order
        for (ScoreDoc docu : top.scoreDocs) {
            Document document = indexSearcher.doc(docu.doc);
            List<String> content = new ArrayList<>();

            String name = document.get("Name_Disease").split(" ",2)[1].split(";",2)[0];
            String properName = name.substring(0,1).toUpperCase() + name.substring(1).toLowerCase();
            content.add(properName);

            content.add("Omim");
            content.add("" + docu.score);

            String bodyParts = document.get("Body_Parts");
            String[] categories = bodyParts.split("§");
            // Skipping the first element of the split, because it will always be empty
            for (int i = 1; i<categories.length; i++) {
                String category = categories[i];

                String[] tokens = category.split(":");
                String[] symptoms = tokens[tokens.length-1].split(";");
                for (String localSymptom : symptoms) {
                    if (contains(localSymptom.toLowerCase(), symptom)) {
                        content.add(localSymptom.split("[ ]+",2)[1]);
                    }
                }
            }
            docs.add(content);
        }
        return docs;
    }

    // Function used to make a proper print of the documents of a list
    public static void printDocs(List<List<String>> docs) {
        for (List<String> content : docs) {
            System.out.println("Name : " + content.get(0));
            if (docs.size()>3) {
                System.out.print("Symptoms : " + content.get(3));
                for (int i = 4; i < content.size(); i++) {
                    System.out.print("; " + content.get(i));
                }
            }
            System.out.println();
            System.out.println("Note : " + content.get(2));
            System.out.println("Source : " + content.get(1));
            System.out.println();
        }
    }

    public static List<Document> searchName(String name, Analyzer analyzer, Directory index, int hits) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("Name_Disease", analyzer).parse(name);
        TopDocs top = indexSearcher.search(q1, hits);
        ArrayList<Document> docs = new ArrayList<>();
        for (ScoreDoc docu : top.scoreDocs) {
            docs.add(indexSearcher.doc(docu.doc));
        }
        return docs;
    }

    // and is complicated : have to delete for each token the documents not present in the other tokens
    public static List<List<String>> searchSymptomAND(String symptom, Analyzer analyzer, Directory index, int hits) throws Exception {
        if (symptom.contains("&")) {
            String[] andTokens = symptom.split(" & ");
            List<List<String>> basis = searchSymptomOR(andTokens[0], analyzer, index, hits*10);
            List<Integer> toRemove = new ArrayList<>();
            /*
             For each AND token result (given by the OR function) we will search for all of the documents in the basis list
             if they are also present in the other queries, and only add them in this case
             */
            for (int i=1; i<andTokens.length; i++) {
                List<List<String>> local = searchSymptomOR(andTokens[i], analyzer, index, hits*10);
                for (List<String> baseDocument : basis) {
                    List<String> names = getNames(local);
                    if (names.contains(baseDocument.get(0))) {
                        int trouve = names.indexOf(baseDocument.get(0));

                        // First adding the new symptoms we found for the given disease
                        addToBasis(baseDocument, local.get(trouve));

                        // Then updating the score of the disease
                        float scoreLocal = Float.parseFloat(local.get(trouve).get(2));
                        float scoreBasis = Float.parseFloat(baseDocument.get(2));
                        float newScore = (scoreBasis + scoreLocal)/2;
                        baseDocument.set(2, "" + newScore);
                    } else {
                        // We cannot remove directly, else it would throw an excpetion, so we will remove later
                        int remove = basis.indexOf(baseDocument);
                        if (!toRemove.contains(remove)) {
                            toRemove.add(remove);
                        }
                    }
                }
            }
            removeAll(basis, toRemove);
            if (basis.size()>hits) {
                basis = basis.subList(0,hits-1);
            }
            return basis;
        } else {
            return searchSymptomOR(symptom, analyzer, index, hits);
        }
    }

    // Or is easier than and, because we add the documents anyway
    public static List<List<String>> searchSymptomOR(String symptom, Analyzer analyzer, Directory index, int hits) throws Exception {
        // Running the complex code only if the symptom contains a '/'
        if (symptom.contains("/")) {
            String[] orTokens = symptom.split(" / ");
            List<List<String>> basis = searchSymptomOR(orTokens[0], analyzer, index, hits);
            /*
            For each part of the orTokens array, the code will try to find and add all the matched documents for each query
            the program uses getNames in order to avoid adding twice the same document
             */
            for (int i=1; i<orTokens.length; i++) {
                List<List<String>> local = searchSymptomOR(orTokens[i], analyzer, index, hits*10);
                for (List<String> localDocument : local) {
                    List<String> names = getNames(basis);
                    if (names.contains(localDocument.get(0))) {
                        int trouve = names.indexOf(localDocument.get(0));

                        // First adding the new symptoms we found for the given disease
                        addToBasis(basis.get(trouve), localDocument);

                        // Then updating the score of the disease
                        float scoreLocal = Float.parseFloat(localDocument.get(2));
                        float scoreBasis = Float.parseFloat(basis.get(trouve).get(2));
                        float newScore = (scoreBasis + scoreLocal)/2 + 1;
                        basis.get(trouve).set(2, "" + newScore);
                    } else {
                        basis.add(localDocument);
                    }
                }
            }

            // Creating a comparator to sort the docs we just have merged
            Comparator<List<String>> comp = (s1, s2) -> {
                float score1 = Float.parseFloat(s1.get(2));
                float score2 = Float.parseFloat(s2.get(2));
                return -Float.compare(score1, score2);
            };

            basis.sort(comp);
            if(basis.size()>hits) {
                basis = basis.subList(0, hits-1);
            }
            return basis;
        } else {
            return searchSymptomSTAR(symptom, analyzer, index, hits);
        }
    }

    /* To manage the _, we will first try to delete it if it is at the beginning or the
    end of the string, and then just replaces all its occurrences with '*'
     */
    public static List<List<String>> searchSymptomSTAR(String symptom, Analyzer analyzer, Directory index, int hits) throws Exception {
        int n = symptom.length();

        // First deleting if the '_' is the first non-space character (eg " _ pain")
        int deb = 0;
        String clean = symptom;
        while (deb<n && clean.charAt(deb) == ' ') {
            deb++;
        }
        if (deb == n) {
            throw new Exception("Query on empty string");
        }
        if(clean.charAt(deb) == '_') {
            clean = clean.replaceFirst("_", "");
            n--;
        }

        // Then we will reverse the string, in order to catch the last (which will become the first)
        // occurrence of '_', and again, deleting it if it's the first non-space character
        clean = reverse(clean);
        int fin = 0;
        while (fin<n && clean.charAt(fin) == ' ') {
            fin++;
        }
        if(clean.charAt(fin) == '_') {
            clean = clean.replaceFirst("_", "");
        }

        // Finally, we have deleted the annoying '_' characters, we can replace all the other ones
        clean = reverse(clean);
        clean = clean.replaceAll("_", "*");

        return parsedSearchDBS(clean, analyzer, index, hits);
    }

    // Function used to avoid adding twice the same document : returns all the names of the current stocked documents
    public static List<String> getNames(List<List<String>> docs) {
        List<String> names = new ArrayList<>();
        for (List<String> doc : docs) {
            names.add(doc.get(0));
        }
        return names;
    }

    // Function that adds a document to the "basis" document, only if it isn't present yet
    public static void addToBasis(List<String> baseDoc, List<String> localDoc) {
        for (int i=3; i<localDoc.size(); i++) {
            String symptom = localDoc.get(i);
            if (!baseDoc.contains(symptom)) {
                baseDoc.add(symptom);
            }
        }
    }

    // Function to remove all the indexes given in the 2nd argument
    public static void removeAll(List<List<String>> basis, List<Integer> toRemove) {
        // Must scan the list upside down, or it won't remove the right indexes
        for (int i=toRemove.size()-1; i>-1; i--) {
            basis.remove(toRemove.get(i).intValue());
        }
    }

    public static String reverse(String str) {
        StringBuilder rev = new StringBuilder();
        int n = str.length();
        for (int i=0; i<n; i++) {
            rev.append(str.charAt(n-i-1));
        }
        return rev.toString();
    }

    // Function recreating the "contains" string method, but now manages the "*" by splitting
    // Return a "merge"
    public static boolean contains(String big, String small) {
        String[] tokens = small.split("\\*");
        for (String part : tokens) {
            if (!big.contains(part)) {
                return false;
            }
        }
        return true;
    }

    public static Directory createIndex(Analyzer analyzer) {
        // Creating index with the usual format : opening file, creating the Indexwriters, then adding documents, ...
        File dir = new File(INDEX_DIR_PATH);
        if (dir.exists()) {
            Utils.deleteDir(dir);
        }

        try {
            Directory index = FSDirectory.open(Paths.get(INDEX_DIR_PATH));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            File file = new File(FILE_PATH);
            indexDocBis(w, file);
            w.close();
            return index;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Directory loadIndex() {
        try {
            // Just looding the index from xhere it is supposed to be
            return FSDirectory.open(Paths.get(INDEX_DIR_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void indexDocBis(IndexWriter writer, File file) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String str = br.readLine();
            while (!str.equals("*THEEND*")) {

                if (!str.equals("*RECORD*")) {
                    throw new Exception("Omim.txt was corrupted");
                } else {
                    // If the line is indeed *RECORD*, we can start reading the record information
                    str = br.readLine();
                }

                Document doc = new Document();
                while(!str.equals("*THEEND*") && !str.equals("*RECORD*")) {
                    if (str.equals("*FIELD* TI")) {
                        doc.add(new TextField("Name_Disease", br.readLine(), Field.Store.YES));
                        str = br.readLine();
                    } else if (str.equals("*FIELD* CS")) {
                        str = br.readLine();
                        StringBuilder buffer = new StringBuilder();

                        // First, we get the first non-empty line
                        if (str.equals("")) {
                            str = br.readLine();
                        }

                        while(str.charAt(0) != '*') {
                            if (str.charAt(str.length()-1) == ':') {
                                buffer.append("§");
                                buffer.append(str);
                            } else {
                                if (!(str.charAt(str.length()-2) == ']')) {
                                    buffer.append(str);
                                }
                            }
                            str = br.readLine();
                            if (str.equals("")) {
                                str = br.readLine();
                            }
                        }
                        doc.add(new TextField("Body_Parts", buffer.toString(), Field.Store.YES));
                    } else {
                        str = br.readLine();
                    }
                }
                writer.addDocument(doc);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void indexDoc(IndexWriter writer, File file) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            /* This document has a particular format : each disease is headed by the token *RECORD*,
            each record has a certain number of fields, headed by *FIELD*, and finally, the document
            ends with *THEEND*.
            According to the mapping, we will only keep 4 fields : NO representing the ID of the disease,
            TI representing the name, CS containing a list of symptoms for different body parts, and TX
            containing 3 sub-fields : DESCRIPTION, CLINICAL FEATURES, and INHERITANCE. For the moment,
            we will keep only the DESCRIPTION sub-field.
             */

            String str = br.readLine();
            /* In order to scan the whole document, we will use While loops, because we can't know the number
            of line of each section in advance */
            while (!str.equals("*THEEND*")) {  // Continue reading while we don't reach the end of the document

                /* With the while structure we chose, we only leave the most inner loop when we encounter
                an other *RECORD* section, which means we finished reading the current disease, and can
                move on to the next one (see below)
                For this reason, we immediately throw an exception is the line encountered isn't *RECORD*
                Finally, in order to be able to get out of the loops, we have to add the case when we reach
                *THEEND* as a condition of the while
                */
                if (!str.equals("*RECORD*")) {
                    throw new Exception("Omim.txt was corrupted");
                } else {
                    // If the line is indeed *RECORD*, we can start reading the record information
                    str = br.readLine();
                }
                Document doc = new Document();
                while (!str.equals("*RECORD*") && !str.equals("*THEEND*")) {
                    String[] tokens = str.split(" ", 2);
                    String field = tokens[0];
                    String fieldName = tokens[1];
                    StringBuilder buffer = new StringBuilder();
                    if (!field.equals("*FIELD*")) {
                        throw new Exception("Omim.txt was corrupted");
                    } else {
                        str = br.readLine();
                        field = str.split(" ", 2)[0];
                    }

                    /* Same principle : while loop to catch all the lines of each *FIELD*, then we add the
                    line to the buffer (doing this way removes all line breaks, and replaces them by spaces)
                     */
                    while (!field.equals("*FIELD*") && !str.equals("*RECORD*") && !str.equals("*THEEND*")) {
                        buffer.append(" ").append(str);
                        str = br.readLine();
                        /* Before going through the next loop passage, we retrieve the first word of the next
                        line, in order to check the different loop conditions
                         */
                        field = str.split(" ", 2)[0];
                    }

                    // Extraction and indexing of TI
                    if (fieldName.equals("TI")) {
                        // The name is directly in the buffer
                        doc.add(new TextField("Name_Disease", buffer.toString(), Field.Store.YES));
                    }

                    // Extraction and indexing of the raw CS
                    if (fieldName.equals("CS")) {
                        // CS is a list of lists, but we will only add it as full raw text,
                        // this is enough for the queries we will have to make after
                        doc.add(new TextField("Body_Parts", buffer.toString(), Field.Store.YES));
                        doc.add(new TextField("Symptom_Nb", "" + buffer.toString().split("[:;]").length, Field.Store.YES));
                    }
                }
                // On ajoute le document à l'index dès qu'on rencontre un autre *RECORD* (et donc on va passer au suivant)
                writer.addDocument(doc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
