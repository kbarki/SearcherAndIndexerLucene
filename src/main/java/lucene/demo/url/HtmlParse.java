package lucene.demo.url;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;

import org.xml.sax.SAXException;

/**
 * This class uses apache Tika to parse html files or the content of web pages
 */
public class HtmlParse {

	// this method return the content of html web page
   public static String parseHtmlWebPage(URL urlPageHtml) throws IOException,SAXException, TikaException {

      BodyContentHandler handler = new BodyContentHandler();
      Metadata metadata = new Metadata();
      
      URL url = urlPageHtml;
	  InputStream inputstream = url.openStream();
      ParseContext pcontext = new ParseContext();
      
      //Html parser 
      HtmlParser htmlparser = new HtmlParser();
      htmlparser.parse(inputstream, handler, metadata,pcontext);
      String[] metadataNames = metadata.names();
      
      //FileInputStream inputstream = new FileInputStream(new File("/home/user/htmlFiles/test.html"));
      // URL url = new URL("https://algs4.cs.princeton.edu/42digraph/WebCrawler.java.html");
      
       return handler.toString();
   }
   
	// this method return the content of html file
   public static String parseHtmlFile(File f) throws IOException,SAXException, TikaException {

	      BodyContentHandler handler = new BodyContentHandler();
	      Metadata metadata = new Metadata();
	      FileInputStream inputStream = new FileInputStream(f);
	      ParseContext pcontext = new ParseContext();
	      
	      //Html parser 
	      HtmlParser htmlparser = new HtmlParser();
	      htmlparser.parse(inputStream, handler, metadata,pcontext);
	      String[] metadataNames = metadata.names();
	    
	       return handler.toString();
	   }
   
   public static boolean isValidUrl(String url)
   {
       // Try creating a valid URL 
       try {
           new URL(url).toURI();
           return true;
       }       
       // If there was an Exception while creating URL object
       catch (Exception e) {
           return false;
       }
   }
}
