package extractor;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.CoarseNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph.OutputFormat;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Test the analysis results of CoreNLP in Chinese and English
 */
public class TestCoreNLP {
	
	public static void main(String[] args){
		TestCoreNLP test = new TestCoreNLP();
//		test.analysisForChinese();
		test.analysisForEnglish();
	}
	
	public void analysisForChinese() {
		String text = "以后操作应该不是很方便";
		Properties props = new Properties();
    	try {
    		String properties_path = System.getProperty("user.dir")+"/src/main/resources/StanfordCoreNLP-chinese.properties";
			props.load( new FileInputStream(properties_path) );
		} catch (IOException e) {
			e.printStackTrace();
		}
    	StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    	long startTime = System.currentTimeMillis();
    	Annotation document = new Annotation(text);
    	pipeline.annotate(document);
    	List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    	for (CoreMap sentence : sentences) {
    		for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)){
    			int index = token.index();
    			int beginPosition = token.beginPosition();
    			int endPosition = token.endPosition();
    			String word = token.word();
    			String lemma = token.lemma();
    			String pos = token.tag();
    			String ner = token.ner();
    			System.out.println(index 
    					         + " ["+beginPosition+","+endPosition + "] " 
    					         + word + " "+ lemma + " " + pos + " " + ner);
    		}
    		// 句子的解析树
    		Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
    		System.out.println("句子的解析树:");
    		tree.pennPrint();
    		// 句子的依赖图
    		SemanticGraph graph =sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
    		System.out.println("句子的依赖图");
    		System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	}
    	long endTime = System.currentTimeMillis();long time = endTime - startTime;
    	System.out.println("The analysis lasts " + time + " seconds * 1000");
    	Map<Integer, CorefChain> corefChains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    	if (corefChains == null) {
    		return;
    	}
    	for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
    		System.out.println("Chain " + entry.getKey() + " ");
    		for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
    			// We need to subtract one since the indices count from 1 but the Lists start from 0
    			List<CoreLabel> tokens = sentences.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
    			// We subtract two for end: one for 0-based indexing, and one because we want last token of mention not one following.
    			System.out.println("  " + m + ", i.e., 0-based character offsets [" + tokens.get(m.startIndex - 1).beginPosition()+", " + tokens.get(m.endIndex - 2).endPosition() + ")");
    		}
    	}
	}
	
	public void analysisForEnglish() {
		String text = "a cheap , decently rugged , light weight notebook";
		Properties props = new Properties();
	    props.setProperty("annotators","tokenize,ssplit,pos,lemma,parse,ner");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    int beginindex;
	    int endindex;
	    String word = null;
	    String pos = null;
	    String lemma = null;
	    String ner = null;
	    Map<Integer, CorefChain> corefChainMap = document.get(CorefChainAnnotation.class);
	    if( corefChainMap!=null && corefChainMap.size()!=0 ) {
	    	for(Map.Entry<Integer, CorefChain> entry:corefChainMap.entrySet() ) {
	   	 		System.out.println(entry.toString());
	    	}
	    }
	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {		
	        SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	        Tree tree = sentence.get(TreeAnnotation.class);
	        System.out.println(tree.toString());
	    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
	    		beginindex=token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
	    		endindex=token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
	    		endindex = token.endPosition();
	    		word = token.word();
	    		pos = token.get(PartOfSpeechAnnotation.class);
	    		ner = token.get(CoarseNamedEntityTagAnnotation.class);
	    		lemma = token.lemma();
	    		ner = token.ner();
	    		System.out.println("["+beginindex+","+endindex+"]."+lemma+" | "+pos+" | "+ner);
	    	}
	    	System.out.println(sentence.toString());
			System.out.println("Dependency Graph:\n " +dependencies.toString(SemanticGraph.OutputFormat.READABLE));
			IndexedWord root = dependencies.getFirstRoot();
			System.out.println(root);
			List<IndexedWord> rootChildren = dependencies.getChildList(root);
			for(int i=0;i<rootChildren.size();i++) {
				IndexedWord child = rootChildren.get(i);
				SemanticGraphEdge edge = dependencies.getEdge(root,child);					
				System.out.println(edge.getRelation());
			}
			System.out.println( dependencies.getChildList(root) );
		} 
	}
	
}