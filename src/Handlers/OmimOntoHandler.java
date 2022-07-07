package Handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import Utils.Utils;

public class OmimOntoHandler {
    static final String INDEX_DIR_PATH = "indexes/omim_onto_index";
    static final String FILE_PATH = "data/OMIM/omim_onto.csv";

    public static Directory createIndex(Analyzer analyzer){
        try {
            Directory index = FSDirectory.open(Paths.get(INDEX_DIR_PATH));
            createIndex(analyzer, index);
            return index;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Directory loadIndex(){
        try {
            return FSDirectory.open(Paths.get(INDEX_DIR_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Document> searchDrugByClassId(String classId, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("ClassId", classId));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static ArrayList<Document> searchDrugByCui(String cui, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("Cui", cui));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static ArrayList<Document> searchDrugByPreferedLabel(String label, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("PreferedLabel", label));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static void createIndex(Analyzer analyzer, Directory index){
        File dir = new File(INDEX_DIR_PATH);

        if (dir.exists()) {
            Utils.deleteDir(dir);
        }

        File file = new File(FILE_PATH);
        if (!file.exists() || !file.canRead()) {
            System.out.println("File '" +file.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {

            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            IndexWriter writer = new IndexWriter(index, config);

            indexOmimOnto(writer, file);
            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }


    /**
     * indexer et stocker CLASS_ID
     * indexer CUI, PreferedLabel, Synonyms
     * @param writer
     * @param file
     * @throws IOException
     */
    static void indexOmimOnto(IndexWriter writer, File file) throws IOException {
        int eltCount = 0;
        if (!file.canRead() && file.isDirectory())
            return;


        // Read CSV
        try {
            BufferedReader br = new BufferedReader(new FileReader(FILE_PATH));
            String line = "";
            String cvsSplitBy = ",";

            while((line = br.readLine()) != null){

                String[] ligneParsee = line.split(cvsSplitBy, -1);

                //write the index
                // make a new, empty document
                Document doc = new Document();
                //add 3 fields to it

                doc.add(new TextField("ClassId", getClassId(ligneParsee[0]), Field.Store.YES)); //indexed
                doc.add(new TextField("Cui", ligneParsee[5], Field.Store.NO)); // indexed
                doc.add(new TextField("PreferedLabel", ligneParsee[1], Field.Store.NO)); // indexed
                doc.add(new TextField("Synonyms", ligneParsee[2], Field.Store.NO)); // indexed
                //System.out.println(id+" "+genericName);
                if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    writer.addDocument(doc);
                } else {
                    writer.updateDocument(new Term("path", file.getPath()), doc);
                }

                eltCount++;

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(eltCount + " elements have been added to the index " + System.getProperty("user.dir") + "/" + INDEX_DIR_PATH);
    }

    public static String getClassId(String URLclassId){
        String[] ligneParsee = URLclassId.split("/");
        int len = ligneParsee.length;
        return ligneParsee[len -1];
    }


    public static void printDocuments(ArrayList<Document> documents){
        System.out.println("======================================================");
        for(Document d : documents){
            System.out.println(d.get("ClassId") + "\n\t");
        }
    }
}
