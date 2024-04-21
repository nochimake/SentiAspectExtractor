package extractor.textExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import extractor.coreNLPRules.EnglishCoreNLPRules;
import extractor.textParser.EnglishTextParser;

/**
 * English aspect extractor
 */
public class EnglishTextExtractor extends TextExtractor{
	
	public void init() {
		textParser = new EnglishTextParser();
		NLPRule = new EnglishCoreNLPRules();
		super.init();
	}
	
	public String preprocessInputText(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
		return enTextTokenMapping(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
	}
	
	private String[] abbrArr = {"t","d","m","s","re","ve","ll"};
	private String[] listSymbolArr = {"+","-","*"};
	private boolean isNumeric(String str) {
        return StringUtils.isNumeric(str);
    }
	private String enTextTokenMapping(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
		String optimizedText = "";
		String[] elems = text.split("\\s+");
		int[] beginPositionArr = new int[elems.length];
		int[] endPositionArr = new int[elems.length];
		for(int i=0;i<elems.length;i++) {
			String elem = elems[i];
			String preElem = i-1>=0 ? elems[i-1]:"";
			String nextElem = i+1<elems.length ? elems[i+1]:"";
			//不预处理文本
			if( !OPT.isTextPreprocessing ) {
				if( i!=0 ) {
					optimizedText += " ";
				}
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
				continue;
			}
			//开始进行文本预处理：
			//检查到缩写符号：
			if( elem.equals("'") && NLPRule.isHave(nextElem,abbrArr) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
				startPosition = optimizedText.length();
				beginPositionArr[i+1] = startPosition;	
				optimizedText += nextElem;
				endPositionArr[i] = optimizedText.length();
				i++;
			}
			//检查到以特殊符号为开头,则删除
			else if( i==0 && NLPRule.isHave(elem,listSymbolArr) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“*” e.g. "i would * not * recommend this laptop .",该符号大概率会影响分析结果,删除
			else if( elem.equals("*") && !isNumeric(preElem) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“`”,该符号大概率会影响分析结果,删除
			else if( elem.toLowerCase().equals("`") ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“- - ”
			else if( elem.equals("-") && preElem.equals("-") ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
			}
			else {
				if( i!=0 ) {
					optimizedText += " ";
				}
				//检查错误写法“im”
				if( elem.toLowerCase().equals("im") ) {
					int startPosition = optimizedText.length();
					beginPositionArr[i] = startPosition;
					elem = elem.replace("m", "'m");
					optimizedText += elem;
					endPositionArr[i] = optimizedText.length();
				}
				//检查“&”,该符号大概率会影响分析结果
//				else if( elem.toLowerCase().equals("&") ) {
//					int startPosition = optimizedText.length();
//					beginPositionArr[i] = startPosition;
//					optimizedText += "and";
//					endPositionArr[i] = optimizedText.length();
//				}
				else {
					int startPosition = optimizedText.length();
					beginPositionArr[i] = startPosition;
					optimizedText += elem;
					endPositionArr[i] = optimizedText.length();
				}
			}
		}
		//System.out.println(optimizedText);
	    ArrayList<Integer> beginPositionList = new ArrayList<Integer>();
		ArrayList<Integer> endPositionList = new ArrayList<Integer>();
		ArrayList<CoreLabel> tokenList = new ArrayList<CoreLabel>();
		Annotation document = new Annotation(optimizedText);
		textParser.spliter.annotate(document);
		List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
	    		int beginPosition = token.beginPosition();
	    		int endPosition = token.endPosition();
	    		beginPositionList.add( beginPosition );
	    		endPositionList.add( endPosition );
	    		tokenList.add(token);
	    	}
	    }
	    
	    for(int i=0;i<beginPositionArr.length;i++) {
	    	int oriIndex = i;
	    	int beginPosition = beginPositionArr[i];
	    	int corTokenIndex = -1;
	    	for(int j=0;j<beginPositionList.size();j++) {
	    		int ti_beginPosition = beginPositionList.get(j);
	    		int ti_endPosition = endPositionList.get(j);
	    		if( ti_beginPosition<=beginPosition && beginPosition<=ti_endPosition ) {
	    			corTokenIndex = j;
	    			List<Integer> indexList = new ArrayList<Integer>();
	    			if( oriIndexTokenIndexMap.containsKey(oriIndex) ) {
	    				indexList = oriIndexTokenIndexMap.get(oriIndex);
	    			}
	    			indexList.add(corTokenIndex);
	    			oriIndexTokenIndexMap.put(oriIndex, indexList);
	    		}
	    	}
	    }
	    
	    for(int i=0;i<tokenList.size();i++) {
	    	int tokenIndex = i;
	    	int beginPosition = beginPositionList.get(tokenIndex);
	    	int corOriIndex = -1;
	    	for(int j=0;j<beginPositionArr.length;j++) {
	    		int ti_beginPosition = beginPositionArr[j];
	    		int ti_endPosition = endPositionArr[j];
	    		if( ti_beginPosition<=beginPosition && beginPosition<=ti_endPosition ) {
	    			corOriIndex = j;
	    			List<Integer> indexList = new ArrayList<Integer>();
	    			if( tokenIndexOriIndexMap.containsKey(tokenIndex) ) {
	    				indexList = tokenIndexOriIndexMap.get(tokenIndex);
	    			}
	    			indexList.add(corOriIndex);
	    			tokenIndexOriIndexMap.put(tokenIndex, indexList);
	    		}
	    	}
	    }
	    
//	    for(int i=0;i<tokenList.size();i++) {
//	    	CoreLabel token = tokenList.get(i);
//	    	String word = token.word();
//	    	int oriIndex = tokenIndexOriIndexMap.get(i);
//	    	String oriWord = elems[oriIndex];
//    		System.out.println(word+":"+oriWord);
//	    }
	    
		return optimizedText;
	}
	
	private Pattern en_pattern = Pattern.compile("[a-zA-Z]");
	private boolean isContainEN(String text) {
		return en_pattern.matcher(text).find();
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
    		//快捷选择：根据尾词的词性进行快捷选择
    		IndexedWord endOpinionNode = opinion.get(size-1);
    		if( NLPRule.isAdj(endOpinionNode.tag()) || NLPRule.isVerb(endOpinionNode.tag()) || NLPRule.isNoun(endOpinionNode.tag()) ) {
    			return endOpinionNode;
    		}
    		//根据依赖关系进行选择
    		int maxDegreeNum = -1;
    		IndexedWord maxDegreeNode = null;
    		for(int i=size-1;i>=0;i--) {
    			IndexedWord opinionTermNode = opinion.get(i);
    			String word = opinionTermNode.word().toLowerCase();
    			int degree = 0;
    			if( isContainEN(word) ) {
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
