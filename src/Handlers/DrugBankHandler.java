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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import Utils.Utils;

public class DrugBankHandler {

    static final String INDEX_DIR_PATH = "indexes/drug_bank_index";
    static final String FILE_PATH = "data/DRUGBANK/drugbank.xml";

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

    public static ArrayList<Document> searchDrugBySideEffect(String sideEffect, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query1 = new TermQuery(new Term("Toxicity", sideEffect));
        Query query2 = new TermQuery(new Term("Pharmacology", sideEffect));
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

    public static ArrayList<Document> searchDrugByIndication(String indication, Analyzer analyzer, Directory index, int hits) throws IOException, ParseException {

        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("Indication", indication));

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

            indexDrugbank(writer, file);
            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }


    /**
     * stocker name,
     * indexer toxicity, pharmacodynamics, indication
     * @param writer
     * @param file
     * @throws IOException
     */
    static void indexDrugbank(IndexWriter writer, File file) throws IOException {
        int eltCount = 0;
        if (!file.canRead() && file.isDirectory())
            return;

        // each line of the file is a new document
        try {

            /*
            0) name      1) toxicity     2) indication     3) pharmacodynamics
             */
            String[] data = new String[4];
            for (int i = 0; i < data.length; i++) {
                data[i] = "";
            }

            XMLInputFactory xmlif = XMLInputFactory.newInstance();

            XMLEventReader xmler = xmlif.createXMLEventReader(new FileReader(file));

            // Index in data array
            int dataIndex = -1;

            int xmlNestingLevel = -2;

            XMLEvent event;
            while (xmler.hasNext()) {
                event = xmler.nextEvent();
                if (event.isStartElement()) {
                    xmlNestingLevel++;
                    switch (event.asStartElement().getName().getLocalPart()) {
                        /*case "drugbank-id":
                            Attribute typeAttribute = event.asStartElement().getAttributeByName(new QName("primary"));
                            if (typeAttribute != null && typeAttribute.getValue().equals("true"))
                                dataIndex = 0;
                            break;*/
                        case "name":
                            if(xmlNestingLevel == 1)
                                dataIndex = 0;
                            else
                                dataIndex = -1;
                            break;
                        case "toxicity":
                            dataIndex = 1;
                            break;
                        case "indication":
                            dataIndex = 2;
                            break;
                        case "pharmacodynamics":
                            dataIndex = 3;
                            break;
                        default:
                            dataIndex = -1;
                            break;
                    }

                } else if (event.isCharacters()) {
                    if (!event.asCharacters().isWhiteSpace() && dataIndex != -1) {
                        if(data[dataIndex] != "")
                            data[dataIndex] += " | " + event.asCharacters().getData();
                        else
                            data[dataIndex] = event.asCharacters().getData();
                    }
                } else if (event.isEndElement()) {
                    xmlNestingLevel -= 1;
                    if (xmlNestingLevel==-1 && event.asEndElement().getName().getLocalPart().equals("drug")) {
                        //write the index
                        // make a new, empty document
                        Document doc = new Document();
                        //add 3 fields to it

                        doc.add(new TextField("name", data[0], Field.Store.YES)); // stored not indexed
                        doc.add(new TextField("Toxicity", data[1], Field.Store.YES)); // indexed
                        doc.add(new TextField("Indication", data[2], Field.Store.YES)); // indexed
                        doc.add(new TextField("Pharmacology", data[3], Field.Store.YES)); // indexed
                        //System.out.println(id+" "+genericName);
                        if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                            writer.addDocument(doc);
                        } else {
                            writer.updateDocument(new Term("path", file.getPath()), doc);
                        }

                        eltCount++;

                        for (int i = 0; i < data.length; i++) {
                            data[i] = "";
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        //System.out.println(eltCount + " elements have been added to the index " + System.getProperty("user.dir") + "/" + INDEX_DIR_PATH);
    }


    public static void printDocuments(ArrayList<Document> documents){

        for(Document d : documents){
            System.out.println(d.get("name") + "\n\t"+ d.get("Toxicity") + "\n\t"+ d.get("Indication") + "\n\t"+ d.get("Pharmacology"));
            System.out.println("======================================================");
        }
    }

}
