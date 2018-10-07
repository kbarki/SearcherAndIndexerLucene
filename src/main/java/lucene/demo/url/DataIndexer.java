package lucene.demo.url;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

/**
 * This application creates an Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */

public class DataIndexer {
	
  // Apache Lucene supplies a large family of Analyzer classes, The most common is the StandardAnalyzer
  // we can use the class FrenchAnalyzer to analyze the French language
  private static StandardAnalyzer analyzer = new StandardAnalyzer();

  private IndexWriter writer;
  private ArrayList<File> queue = new ArrayList<File>();
  private static Directory dir;

  /**
   * Constructor
   * @param indexDir the name of the folder in which the index should be created
   * @throws java.io.IOException when exception creating index.
   */
  DataIndexer(String indexDir) throws IOException {
    
	// In the new version of Lucene, FSDirectory.open call takes a Path argument, not a File
	File ipath = new File(indexDir);  
     dir = FSDirectory.open(ipath.toPath());

    // IndexWriterConfig Holds all the configuration that is used to create an IndexWrit
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    writer = new IndexWriter(dir, config);
  }
  
  public static void main(String[] args) throws IOException {
    System.out.println("Enter the path where the index will be created:");

    String indexLocation = null;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String s = br.readLine();

    DataIndexer indexer = null;
    try {
      indexLocation = s;
      indexer = new DataIndexer(s);
    } catch (Exception ex) {
      System.out.println("Cannot create index..." + ex.getMessage());
      System.exit(-1);
    }

    //read input from user until he enters q for quit
    while (!s.equalsIgnoreCase("q")) {
      try {
        System.out.println("Enter the path or url to add into the index (q=quit): ");
        System.out.println("[Acceptable file types: .txt, .html]");
        s = br.readLine();
        if (s.equalsIgnoreCase("q")) {
          break;
        }
        //check if the url is valid
        if (HtmlParse.isValidUrl(s)) {
          //try to add html web page into the index. indexUrl is called
          System.out.println("---------valid url----------");
          indexer.indexUrl(s, indexLocation);
        }
        else {
          try {
        	//try to add file into the index, indexFileOrDirectory is called
        	indexer.indexFileOrDirectory(s, indexLocation);
          }catch (Exception c) {
             System.out.println("please enter a path or a valid url " + s + " : " + c.getMessage());
          }
        }
      }catch (Exception e) {
        System.out.println("Error indexing " + s + " : " + e.getMessage());
      }
    }

    // we have to call the closeIndex, otherwise the index is not created    
    indexer.closeIndex();

    //  search
    IndexReader reader = DirectoryReader.open(FSDirectory.open((new File(indexLocation)).toPath()));
    IndexSearcher searcher = new IndexSearcher(reader);

    s = "";
    while (!s.equalsIgnoreCase("q")) {
      try {
        System.out.println("Enter the search query (q=quit):");
        s = br.readLine();
        if (s.equalsIgnoreCase("q")) {
          break;
        }
        Query q = new QueryParser( "contents", analyzer).parse(s);
        TopDocs results = searcher.search(q, 10); //Finds the top n hits for query, in this case n=10
        ScoreDoc[] hits = results.scoreDocs;

        // display results
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
          int docId = hits[i].doc;
          Document d = searcher.doc(docId);
          System.out.println((i + 1) + ". " + d.get("path") + " score=" + hits[i].score);
        }

      } catch (Exception e) {
        System.out.println("Error searching " + s + " : " + e.getMessage());
      }
    }

  }

  /**
   * this method is called when we want to index a html web page 
   * @param indexLocation the name of the folder in wish we want to add the index
   * @param url of the html web page
   * @throws java.io.IOException when exception
   */
  public void indexUrl(String url, String indexLocation) throws IOException {  
	  
	  StringReader fr = null;
	  try {
		  URL urlFromString = new URL(url);
		  // we call parseHtmlWebPage to parse the web page 
		  String pageContent = HtmlParse.parseHtmlWebPage(urlFromString);
		  fr = new StringReader(pageContent);
	      
		  // we check if there are indexes in the directory before using BooleanQuery. this step is important
		  // otherwise we will not be able to create the indexes because of the use of BooleanQuery
		  if(DirectoryReader.indexExists(dir)) {
			  // BooleanQuery enable to check if a field already exists
		      BooleanQuery matchingQuery = new BooleanQuery.Builder()
		      .add(new TermQuery(new Term("path", url)), Occur.SHOULD)
		      .build();
	       
		      IndexReader reader = DirectoryReader.open(FSDirectory.open((new File(indexLocation)).toPath()));
		      IndexSearcher searcher = new IndexSearcher(reader);
		      TopDocs results = searcher.search(matchingQuery, 1);
		      ScoreDoc[] hits = results.scoreDocs;
		      
		      Document doc = new Document();
	          doc.add(new TextField("contents", fr));
	          doc.add(new StringField("path", url, Field.Store.YES));
	          
  	      	  // we add the file if it doesn't exist else we update it
		      if (hits.length == 0){
		          writer.addDocument(doc);
		          System.out.println("Added: " + url);
		          System.out.println("");
		  	      System.out.println(" 1 document (url) added.");
		       
		      }else {
		    	  writer.updateDocument(new Term("path", url), doc);
		          System.out.println("updated: " + url);
		         }
		      
		  }else {
			  Document doc = new Document();
	          doc.add(new TextField("contents", fr));
	          doc.add(new StringField("path", url, Field.Store.YES));

	          writer.addDocument(doc);
	          System.out.println("Added: " + url);
	          System.out.println("");
	  	      System.out.println(" 1 document (url) added.");
		  }
	      } catch (Exception e) {
	        System.out.println("Could not add: " + url);
	      } finally {
	        fr.close();
	      }  
	  }

  
  /**
   * this method is called when we want to index a html files 
   * @param fileName the name of a text file or a folder we wish to add to the index
   * @param indexLocation the name of the folder in wish we want to add the index
   * @throws java.io.IOException when exception
   */
  public void indexFileOrDirectory(String fileName, String indexLocation) throws IOException {
    //gets the list of files in a folder (if user has submitted the name of a 
	//folder) or gets a single file name (is user has submitted only the file name) 
    addFiles(new File(fileName));
    int originalNumDocs = writer.numDocs();
    
    for (File f : queue) {
      StringReader fr = null;
      try {
		// we call parseHtmlWebPage to parse the content of the file
        String fileContent = HtmlParse.parseHtmlFile(f);
        fr = new StringReader(fileContent);
        
        //we check if there are indexes in the directory before using BooleanQuery
        if(DirectoryReader.indexExists(dir)) {
			// BooleanQuery enable to check if a field already exists
	        BooleanQuery matchingQuery = new BooleanQuery.Builder()
	        .add(new TermQuery(new Term("path", f.getPath())), Occur.SHOULD)
	        .add(new TermQuery(new Term("filename", f.getName())), Occur.SHOULD)
	        .build();
	        
	        IndexReader reader = DirectoryReader.open(FSDirectory.open((new File(indexLocation)).toPath()));
	        IndexSearcher searcher = new IndexSearcher(reader);
	        TopDocs results = searcher.search(matchingQuery, 1);
	        ScoreDoc[] hits = results.scoreDocs;
	        Document doc = new Document();
	    	doc.add(new TextField("contents", fr));
	    	doc.add(new StringField("path", f.getPath(), Field.Store.YES));
	    	doc.add(new StringField("filename", f.getName(), Field.Store.YES));
	    	// we add the file if it doesn't exist else we update it
	        if (hits.length == 0){
	        	writer.addDocument(doc);
	        	System.out.println("Added: " + f);
	        }
	        else {
	        	System.out.println("updating " + f);
	            writer.updateDocument(new Term("path", f.getPath()), doc);
	        }
        }else {
        	Document doc = new Document();
        	doc.add(new TextField("contents", fr));
        	doc.add(new StringField("path", f.getPath(), Field.Store.YES));
        	doc.add(new StringField("filename", f.getName(), Field.Store.YES));
        	writer.addDocument(doc);
        	System.out.println("Added: " + f);
        }
      }catch (Exception e) {
        System.out.println("Could not add: " + f);
      } finally {
        fr.close();
      }
    }
    
    int newNumDocs = writer.numDocs();
    System.out.println("");
    System.out.println((newNumDocs - originalNumDocs) + " documents added.");

    queue.clear();
  }
  
  // check if file exists and add it to the queue
  private void addFiles(File file) {

    if (!file.exists()) {
      System.out.println(file + " does not exist.");
    }
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        addFiles(f);
      }
    } else {
      String filename = file.getName().toLowerCase();
      // Only index text and html files
      if (filename.endsWith(".html") || filename.endsWith(".txt")) {
        queue.add(file);
      } else {
        System.out.println("ignored file " + filename);
      }
    }
  }

  
  public void closeIndex() throws IOException {
	  try {
		  writer.commit();
		  writer.close();
	  }catch (IOException ex) {
          System.err.println("We had a problem closing the index: " + ex.getMessage());
      } 
  }
}