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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HPOHandler {

    static final String INDEX_DIR_PATH = "indexes/hpo_index";
    static final String FILE_PATH = "data/HPO/hpo.obo";

    //TODO : fonctions de recherche

    public static String getIDbyName(String name, Analyzer analyzer, Directory index) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("Name_Symptom", analyzer).parse('"' + name + '"');
        TopDocs top = indexSearcher.search(q1, 1);
        Document doc = indexSearcher.doc(top.scoreDocs[0].doc);
        return doc.get("ID_Symptom");
    }

    public static String getNameByID(String id, Analyzer analyzer, Directory index) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("ID_Symptom", analyzer).parse('"' + id + '"');
        TopDocs top = indexSearcher.search(q1, 1);
        Document doc = indexSearcher.doc(top.scoreDocs[0].doc);
        return doc.get("Name_Symptom");
    }

    public static Document getDocumentById(String id, Analyzer analyzer, Directory index) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("ID_Symptom", analyzer).parse('"' + id + '"');
        TopDocs top = indexSearcher.search(q1, 1);
        return indexSearcher.doc(top.scoreDocs[0].doc);
    }

    public static List<String> getSynonymsByName(String name, Analyzer analyzer, Directory index) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new QueryParser("Name_Symptom", analyzer).parse('"' + name + '"');
        TopDocs top = indexSearcher.search(q1, 1);
        List<String> synonyms = new ArrayList<>();
        int totalHits = (int) top.totalHits.value;
        if (totalHits>0) {
            Document doc = indexSearcher.doc(top.scoreDocs[0].doc);
            if (doc.get("Synonyms_Symptom") != null) {
                String[] synonymsChains = doc.get("Synonyms_Symptom").split(";");
                // Skipping the last element, because it is always ""
                for (int i=0; i<synonymsChains.length-1; i++) {
                    String synonymToParse = synonymsChains[i];
                    synonyms.add(synonymToParse.split("\"")[1]);
                }
            }
        }
        return synonyms;
    }

    public static Directory createIndex(Analyzer analyzer) {
        File dir = new File(INDEX_DIR_PATH);
        if (dir.exists()) {
            Utils.deleteDir(dir);
        }

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

            /* The parsing of this file is pretty easy : each symptom is under the header [Term]
            and then, each line contains one field and its content after the ':'
            The only thing to be careful about is that some fields may appear multiple times :
            to manage this, once a certain field is found, we will check in the following line
            if it appears again, in which case all those lines would be added into the index as a
            unique String.
             */
            String str = br.readLine();
            // The first step is to skip all the headers of the file
            while (str != null && !str.equals("[Term]")) {
                str = br.readLine();
            }

            // Once the headers are skipped, we can start the main loop
            while (str != null) {
                // As the inner loop is exited only when we reach a [Term] line,
                // we first check if the line hasn't encountered a problem
                if (!str.equals("[Term]")) {
                    throw new Exception("hpo.obo was corrupted");
                } else {
                    str = br.readLine();
                }

                String field = "";
                StringBuilder buffer = new StringBuilder();
                Document doc = new Document();
                // Then, we can start the inner loop
                while (str != null && !str.equals("[Term]")) {
                    String[] tokens = str.split(": ");
                    String content;
                    if (tokens.length>1) {
                        content = tokens[1];
                    } else {
                        content = "";
                    }

                    if (!field.equals(tokens[0])) {
                        // If the field name isn't the same as before, we will add it to the buffer
                        indexField(doc, field, buffer.toString());
                        field = tokens[0];
                        buffer = new StringBuilder();
                    }
                    buffer.append(content).append("; ");
                    str = br.readLine();
                }
                writer.addDocument(doc);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void indexField(Document doc, String field, String buffer) {
        if (field.equals("id")) {
            doc.add(new TextField("ID_Symptom", buffer, Field.Store.YES));
        }
        if (field.equals("name")) {
            doc.add(new TextField("Name_Symptom", buffer, Field.Store.YES));
        }
        if (field.equals("alt_id")) {
            doc.add(new TextField("Alternative_ID_Symptom", buffer, Field.Store.NO));
        }
        if (field.equals("def")) {
            buffer = buffer.replaceFirst("\"", "");
            buffer = (buffer.split("\" "))[0];
            doc.add(new TextField("Description_Symptom", buffer, Field.Store.NO));
        }
        if (field.equals("synonym")) {
            doc.add(new TextField("Synonyms_Symptom", buffer, Field.Store.YES));
        }
        if (field.equals("is_a")) {
            doc.add(new TextField("Parent_Terms", buffer, Field.Store.NO));
        }
    }
}
