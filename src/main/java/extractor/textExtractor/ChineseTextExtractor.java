package extractor.textExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import extractor.Aspect;
import extractor.Opinion;
import extractor.coreNLPRules.ChineseCoreNLPRules;
import extractor.textParser.ChineseTextParser;

/**
 * Chinese aspect extractor
 */
public class ChineseTextExtractor extends TextExtractor{
	
	public void init() {
		textParser = new ChineseTextParser();
		NLPRule = new ChineseCoreNLPRules();
		super.init();
	}
	
	public String preprocessInputText(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
		return chTextTokenMapping(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
	}
	
	private String chTextTokenMapping(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
	    int token_index = 0;
		Annotation document = new Annotation(text);
		textParser.spliter.annotate(document);
		List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//	    		System.out.print(token.word()+" ");
	    		ArrayList<Integer> oriIndexList = new ArrayList<Integer>();
	    		for (int i = token.beginPosition();i<token.endPosition();i++) {
	    			ArrayList<Integer> tokenIndexList = new ArrayList<Integer>();
	    			tokenIndexList.add(token_index);
	    			oriIndexTokenIndexMap.put(i, tokenIndexList);
	    			oriIndexList.add(i);
	    		}
	    		tokenIndexOriIndexMap.put(token_index, oriIndexList);
	    		token_index++;
	    	}
	    }
	    
		return text;
	}
	
	private Pattern ch_pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
	private boolean isContainCH(String text) {
		return ch_pattern.matcher(text).find();
	}
	protected IndexedWord selectCoreOpinionNode(ArrayList<IndexedWord> opinion) {
    	if( opinion==null || opinion.size()==0 ) {
    		return null;
    	}
    	int size = opinion.size();
    	//opinion 只有一个单词
    	if( size==1 ) {
    		IndexedWord coreOpinionNode = opinion.get(0);
    		return coreOpinionNode;
    	}
    	//opinion 含有多个单词
    	else {
    		//根据依赖关系进行选择
    		int maxDegreeNum = -1;
    		IndexedWord maxDegreeNode = null;
    		for(int i=size-1;i>=0;i--) {
    			IndexedWord opinionTermNode = opinion.get(i);
    			String word = opinionTermNode.word().toLowerCase();
    			int degree = 0;
    			if( isContainCH(word) ) {
    				SemanticGraph graph = textParser.getGraphByNode(opinionTermNode);
    				int outDegree = graph.getChildren(opinionTermNode).size();
    				int inDegree = graph.getParents(opinionTermNode).size();
    				degree = inDegree+outDegree;
    			}
    			if( degree>maxDegreeNum ) {
    				maxDegreeNum = degree;
    				maxDegreeNode = opinionTermNode;
    			}
    		}
    		return maxDegreeNode;
    	}
    }

}
