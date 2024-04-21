package extractor.textParser;

import java.util.Properties;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * English text parser
 */
public class EnglishTextParser extends TextParser {
	@Override
	public void spliterInit() {
		if( !isSpliterInit ) {
			Properties props = new Properties();
		    props.setProperty("annotators","tokenize, ssplit");
		    spliter = new StanfordCoreNLP(props);
		    isSpliterInit = true;
		}
	}
	@Override
	public void parserInit() {
		if( !isParserInit ) {
			Properties props = new Properties();
			if( OPT.isUseCoreference ) {
				props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse, depparse, ner, dcoref");
			}
			else {
				props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse, depparse, ner");
			}
		    parser = new StanfordCoreNLP(props);
		    isParserInit = true;
		    System.out.println("Initialize TextParser!");
		}
	}
	@Override
	public String[] getPartitionSymbol() {
		return new String[] {",",":",".","HYPH"};
	}

}
