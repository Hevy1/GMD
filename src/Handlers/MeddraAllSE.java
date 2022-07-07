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
import java.util.List;
import java.util.StringTokenizer;

import Utils.Utils;

public class MeddraAllSE {
    static final String INDEX_DIR_PATH = "indexes/meddra_all_se_index";
    static final String FILE_PATH = "data/MEDDRA/meddra_all_se.tsv";

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

    public static ArrayList<Document> searchAll(Analyzer analyzer, Directory index, int hits) throws IOException {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new MatchAllDocsQuery();
        TopDocs topDocs = searcher.search(query, hits);

        System.out.println("taille topdocs "+topDocs.scoreDocs.length);
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents;
    }

    public static ArrayList<Document> searchSideEffectByCui(String cui, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("cui", cui));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);

        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static ArrayList<Document> searchIndicationById(String id, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("cui_of_meddra_term", id));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);

        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static ArrayList<Document> searchSideEffectByLabel(String label, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("side_effect_name", label));

        TopDocs topDocs = searcher.search(query, hits, Sort.INDEXORDER);
        System.out.println("taille topdocs "+topDocs.scoreDocs.length);

        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        return documents;
    }

    public static ArrayList<Document> searchDrugById(String id, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query1 = new TermQuery(new Term("stitch_compound_id1", id));
        Query query2 = new TermQuery(new Term("stitch_compound_id2", id));
        BooleanQuery booleanQuery = new BooleanQuery.Builder()
                .add(query1, BooleanClause.Occur.SHOULD)
                .add(query2, BooleanClause.Occur.SHOULD)
                .build();

        TopDocs topDocs = searcher.search(booleanQuery, hits, Sort.INDEXORDER);
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

            indexMeddraIndications(writer, file);
            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }


    /**
     * indexer et stocker cui
     * indexer side_effect_name, cui_of_meddra_term, stitch_compound_id1, stitch_compound_id2
     * @param writer
     * @param file
     * @throws IOException
     */
    static void indexMeddraIndications(IndexWriter writer, File file) throws IOException {
        int eltCount = 0;
        if (!file.canRead() && file.isDirectory())
            return;

        try{
            // Read TSV
            StringTokenizer st ;
            BufferedReader TSVFile = new BufferedReader(new FileReader(FILE_PATH));
            String dataRow = TSVFile.readLine(); // Read first line.

            while (dataRow != null){
                st = new StringTokenizer(dataRow,"\t");
                List<String> dataArray = new ArrayList<String>() ;
                while(st.hasMoreElements()){
                    dataArray.add(st.nextElement().toString());
                }
                // Row is complete
                if(dataArray.size() >= 6){
                    //write the index
                    // make a new, empty document
                    Document doc = new Document();
                    //add 3 fields to it

                    doc.add(new TextField("cui", dataArray.get(2), Field.Store.YES)); //indexed and storred
                    doc.add(new TextField("side_effect_name", dataArray.get(4), Field.Store.NO)); // indexed
                    doc.add(new TextField("cui_of_meddra_term", dataArray.get(4), Field.Store.NO)); // indexed
                    doc.add(new TextField("stitch_compound_id1", dataArray.get(0), Field.Store.NO)); // indexed
                    doc.add(new TextField("stitch_compound_id2", dataArray.get(1), Field.Store.NO)); // indexed
                    //System.out.println(id+" "+genericName);
                    if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                        writer.addDocument(doc);
                    } else {
                        writer.updateDocument(new Term("path", file.getPath()), doc);
                    }
                    eltCount++;
                }
                dataRow = TSVFile.readLine(); // Read next line of data.
            }
            // Close the file once all data has been read.
            TSVFile.close();

        }catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(eltCount + " elements have been added to the index " + System.getProperty("user.dir") + "/" + INDEX_DIR_PATH);
    }

    public static void printDocuments(ArrayList<Document> documents){
        System.out.println("======================================================");
        for(Document d : documents){
            System.out.println(d.get("cui")+ "\n\t");
        }
    }
}
