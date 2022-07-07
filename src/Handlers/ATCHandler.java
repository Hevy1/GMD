package Handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ATCHandler {

    static final String INDEX_DIR_PATH = "indexes/ATC_index";
    static final String FILE_PATH = "data/ATC/br08303.keg";

    public static List<Document> searchIdByName(String name, int hits) throws IOException, ParseException {
        Directory idx = FSDirectory.open(Paths.get(INDEX_DIR_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexReader iReader = DirectoryReader.open(idx);
        IndexSearcher iSearcher = new IndexSearcher(iReader);
        Query query = new QueryParser("Name_Drug", analyzer).parse('"' + name + '"');
        TopDocs top = iSearcher.search(query, hits);
        List<Document> docs = new ArrayList<>();
        for (ScoreDoc scoreDoc : top.scoreDocs) {
            docs.add(iSearcher.doc(scoreDoc.doc));
        }
        return docs;
    }

    public static List<Document> searchNameById(String id, int hits) throws IOException, ParseException {
        Directory idx = FSDirectory.open(Paths.get(INDEX_DIR_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexReader iReader = DirectoryReader.open(idx);
        IndexSearcher iSearcher = new IndexSearcher(iReader);
        Query query = new QueryParser("ID_Drug", analyzer).parse(id);
        TopDocs top = iSearcher.search(query, hits);
        List<Document> docs = new ArrayList<>();
        for (ScoreDoc scoreDoc : top.scoreDocs) {
            docs.add(iSearcher.doc(scoreDoc.doc));
        }
        return docs;
    }

    public static Directory createIndex(Analyzer analyzer) {
        try {
            Directory index = FSDirectory.open(Paths.get(INDEX_DIR_PATH));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            File file = new File(FILE_PATH);
            indexDoc(w, file);
            w.close();
            return index;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Directory loadIndex() {
        try {
            return FSDirectory.open(Paths.get(INDEX_DIR_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void indexDoc(IndexWriter writer, File file) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            /* The parsing task is easy : we just have to look all the lines, and if the letter
            indicated at the beginning of the line is 'F', we add the line to the index
             */
            String str = br.readLine();

            // First step is to skip the headers
            while (str.charAt(0) != '!') {
                str = br.readLine();
            }

            str = br.readLine();
            // All the characters (from A to E) are stocked, but not used atm
            String charA = "", charB = "", charC = "", charD = "", charE ="";
            // Now, we can start the loop
            while(str.charAt(0) != '!') {
                char head = str.charAt(0);
                // The switch / case simply uses the 1rt character of the line
                switch (head) {
                    case 'A' :
                        charA = str.split("[ ]+",2)[1];
                        break;
                    case 'B' :
                        charB = str.split("[ ]+", 3)[2];
                        break;
                    case 'C' :
                        charC = str.split("[ ]+", 3)[2];
                        break;
                    case 'D' :
                        charD = str.split("[ ]+", 3)[2];
                        break;
                    case 'E' :
                        String id = str.split("[ ]+", 3)[1];
                        String name = str.split("[ ]+", 3)[2];
                        if (name.charAt(name.length()-1) == ']') {
                            String[] tokens = name.split("\\[");
                            name = revert(tokens);
                        }
                        Document doc = new Document();
                        doc.add(new TextField("ID_Drug", id, Field.Store.YES));
                        doc.add(new TextField("Name_Drug", name, Field.Store.YES));
                        writer.addDocument(doc);
                        break;
                    case 'F' :
                        id = str.split("[ ]+", 3)[1];
                        name = str.split("[ ]+",3)[2];
                        String norm = "";
                        if (name.charAt(name.length()-1) == ')') {
                            String[] tokens = name.split(" \\(");
                            // Norm stocked here if needed
                            norm = tokens[tokens.length-1].replaceFirst("\\)","");
                            name = revertParent(tokens);
                        }
                        doc = new Document();

                        doc.add(new TextField("ID_Drug", id, Field.Store.YES));
                        doc.add(new TextField("Name_Drug", name, Field.Store.YES));
                        /*
                        doc.add(new TextField("Norm", name, Field.Store.NO));
                        doc.add(new TextField("Character_A", charA, Field.Store.NO));
                        doc.add(new TextField("Character_B", charB, Field.Store.NO));
                        doc.add(new TextField("Character_C", charC, Field.Store.NO));
                        doc.add(new TextField("Character_D", charD, Field.Store.NO));
                        doc.add(new TextField("Character_E", charE, Field.Store.NO));*/

                        writer.addDocument(doc);
                        break;
                }

                str = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String revert(String[] tokens) {
        StringBuilder str = new StringBuilder();
        str.append(tokens[0]);
        for (int i=1; i<tokens.length-1; i++) {
            str.append(" [").append(tokens[i]);
        }
        return str.toString();
    }

    public static String revertParent(String[] tokens) {
        StringBuilder str = new StringBuilder();
        str.append(tokens[0]);
        for (int i=1; i<tokens.length-1; i++) {
            str.append(" (").append(tokens[i]);
        }
        return str.toString();
    }
}
