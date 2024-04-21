package extractor.textParser;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Chinese text parser
 */
public class ChineseTextParser extends TextParser {
	
	@Override
	public void spliterInit() {
		if( !isSpliterInit ) {
			Properties props = new Properties();
			try {
				props.load( this.getClass().getResourceAsStream("/StanfordCoreNLP-chinese.properties") );
			} catch (IOException e) {
				e.printStackTrace();
			}
		    props.setProperty("annotators","tokenize, ssplit");
		    spliter = new StanfordCoreNLP(props);
		    isSpliterInit = true;
		}
	}
	@Override
	public void parserInit() {
		if( !isParserInit ) {
			Properties props = new Properties();
			try {
				props.load( this.getClass().getResourceAsStream("/StanfordCoreNLP-chinese.properties") );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if( OPT.isUseCoreference ) {
				props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse, ner, coref");
			}
			else {
				props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse, ner");
			}
	    	parser = new StanfordCoreNLP(props);
		    isParserInit = true;
		    System.out.println("Initialize TextParser!");
		}
	}
	
	@Override
	public String[] getPartitionSymbol() {
		return new String[] {"PU"};
	}
}
