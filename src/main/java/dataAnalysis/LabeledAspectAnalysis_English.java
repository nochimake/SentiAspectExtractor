package dataAnalysis;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import extractor.FileIOs;

/**
 * We use this class to analyze English annotated text, 
 * such as identifying its verb aspect.
 */
public class LabeledAspectAnalysis_English {
	public StanfordCoreNLP parser;
	public String labeledFileName;
	public static void main(String[] args) {
		LabeledAspectAnalysis_English ana = new LabeledAspectAnalysis_English();
		String resourcePath = System.getProperty("user.dir") + "/src/main/resources/";
		String labeledFileName = resourcePath+"laptop_quad_test.tsv";
		ana.verbAspectSearch(labeledFileName);
	}
	
	public void parserInit() {
		Properties props = new Properties();
		props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse");
		props.setProperty("tokenize.whitespace","true");
		parser = new StanfordCoreNLP(props);
	}
	
	public boolean isVerb(String tag) {
		return tag.substring(0,2).equals("VB");
	}
	
    /**
     * Find out which words in the labeled text belong to the verb aspect.
     *
     * @param labeledFileName The name of the labeled file to be used for the search.
     */
	public void verbAspectSearch(String labeledFileName) {
		boolean isFileExists = FileIOs.isFileExists(labeledFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+labeledFileName);
			return;
		}
		System.err.println("Start verbAspectSearch(): "+labeledFileName);
		parserInit();
		HashMap<String,ArrayList<String>> verbAspect_text_map = new HashMap<String,ArrayList<String>>();
		List<String> lineList = FileIOs.readFileGetStringList(labeledFileName);
		for(int i=0;i<lineList.size();i++) {
			String inputText = lineList.get(i);
			int first_tab_index = inputText.indexOf("\t");
			String text = inputText.substring(0,first_tab_index);
			ArrayList<CoreLabel> token_list = getCoreLabelList(text);
			String acos_list_string = inputText.substring(first_tab_index+1,inputText.length());
			ArrayList<ACSOUnit> acso_list = getACOSUnitList(acos_list_string);
			HashSet<int[]> aspect_set = new HashSet<int[]>();
			for(ACSOUnit acso:acso_list) {
				aspect_set.add(acso.getAspect());
			}
			HashMap<Integer,List<Integer>> oriIndexTokenIndexMap = new HashMap<Integer,List<Integer>>();
			HashMap<Integer,List<Integer>> tokenIndexOriIndexMap = new HashMap<Integer,List<Integer>>();
			enTextTokenMapping(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
			for(int[] aspect:aspect_set) {
				int[] token_index = transInputIndexArrToTokenIndexArr(aspect,oriIndexTokenIndexMap);
				for(int j=token_index[0];j<=token_index[1];j++) {
					if( isVerb(token_list.get(j).tag()) ) {
						String key = token_list.get(j).originalText();
						if( !verbAspect_text_map.containsKey(key) ) {
							verbAspect_text_map.put(key, new ArrayList<String>());
						}
						verbAspect_text_map.get(key).add(inputText);
					}
				}
//				if( token_index[0]==token_index[1] && isVerb(token_list.get(token_index[0]).tag()) ) {
//					String key = token_list.get(token_index[0]).originalText();
//					if( !verbAspect_text_map.containsKey(key) ) {
//						verbAspect_text_map.put(key, new ArrayList<String>());
//					}
//					verbAspect_text_map.get(key).add(inputText);
//				}
			}
		}
		for(String aspect:verbAspect_text_map.keySet()) {
			ArrayList<String> value = verbAspect_text_map.get(aspect);
			System.out.println(aspect+": "+value.size());
			for(String text:value) {
				System.out.println(text);
			}
			System.out.println();
		}
	}
	
	private ArrayList<CoreLabel> getCoreLabelList(String text){
		ArrayList<CoreLabel> tokenList = new ArrayList<CoreLabel>();
		Annotation document = new Annotation(text);
    	parser.annotate(document);
	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		tokenList.add(token);
	    	}
        }
	    return tokenList;
	}
	
	private int[] transInputIndexArrToTokenIndexArr(int[] inputIndexOPArr,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap) {
		int startOriIndex = inputIndexOPArr[0];
		int endOriIndex = inputIndexOPArr[1]-1;
		int[] tokenIndexOPArr = new int[2];
		tokenIndexOPArr[0] = oriIndexTokenIndexMap.get(startOriIndex).get(0);
		tokenIndexOPArr[1] = oriIndexTokenIndexMap.get(endOriIndex).get( oriIndexTokenIndexMap.get(endOriIndex).size()-1 );
		return tokenIndexOPArr;
	}
	
	private void enTextTokenMapping(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
		String optimizedText = "";
		String[] elems = text.split("\\s+");
		int[] beginPositionArr = new int[elems.length];
		int[] endPositionArr = new int[elems.length];
		for(int i=0;i<elems.length;i++) {
			String elem = elems[i];
			if( i!=0 ) {
				optimizedText += " ";
			}
			int startPosition = optimizedText.length();
			beginPositionArr[i] = startPosition;
			optimizedText += elem;
			endPositionArr[i] = optimizedText.length();
		}
	    ArrayList<Integer> beginPositionList = new ArrayList<Integer>();
		ArrayList<Integer> endPositionList = new ArrayList<Integer>();
		ArrayList<CoreLabel> tokenList = new ArrayList<CoreLabel>();
		Annotation document = new Annotation(optimizedText);
		parser.annotate(document);
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
	}
	
	Pattern en_acso_pattern = Pattern.compile("(\\d+,\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+,\\d+)");
	private ArrayList<ACSOUnit> getACOSUnitList(String acos_list_string){
		ArrayList<ACSOUnit> acos_list = new ArrayList<ACSOUnit>();
        Matcher matcher = en_acso_pattern.matcher(acos_list_string);
        while (matcher.find()) {
        	String aspect_string = matcher.group(1); 
            String catagory = matcher.group(2);
            String sentiment_string = matcher.group(3);
            String opinion_string = matcher.group(4);
            int[] aspect = stringToIntArray(aspect_string);
            int sentiment = Integer.parseInt(sentiment_string)-1;
            int[] opinion = stringToIntArray(opinion_string);
            ACSOUnit acos = new ACSOUnit(aspect,catagory,sentiment,opinion);
            acos_list.add(acos);
         }
        return acos_list;
	}
	
	private int[] stringToIntArray(String str) {
        str = str.replace("[", "").replace("]", "");
        String[] strArray = str.split(",");
        int[] intArray = new int[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            intArray[i] = Integer.parseInt(strArray[i].trim());
        }
        return intArray;
    }

}
