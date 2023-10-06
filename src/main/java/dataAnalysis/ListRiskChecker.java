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
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import extractor.FileIOs;

/**
 * This is a class for checking potential risks in a word list.
 * The words in "implicitAspectWordList.txt" will not be included as aspect outputs. 
 * "peopleWordList.txt" helps us identify human nouns, 
 * and they will not be included as aspect outputs when the subject is a person.
 * We use this class to detect risks in both of them.
 */
public class ListRiskChecker {
	public StanfordCoreNLP parser;
	public String labeledFileName;
	public HashMap<String,ArrayList<String>> aspect_text_list_map;
	public static void main(String[] args) {
		String path = System.getProperty("user.dir");
		String resourcePath = path+"/src/main/resources/";
		String labeledFileName = resourcePath+"laptop_quad_test.tsv";
		ListRiskChecker riskChecker = new ListRiskChecker();
		riskChecker.initLabeledFile(labeledFileName);
		
		String dictPath = System.getProperty("user.dir")+"/src/main/resources/dictionary/";
		String implicitAspectWordListPath = dictPath + "implicitAspectWordList.txt";
		riskChecker.checkRisk( implicitAspectWordListPath );
		String peopleWordListPath = dictPath + "peopleWordList.txt";
		riskChecker.checkRisk( peopleWordListPath );
	}
	public String getLabeledFileName() {
		return labeledFileName;
	}
	public void setLabeledFileName(String labeledFileName) {
		this.labeledFileName = labeledFileName;
	}
	public void parserInit(String type) {
		if( type.equals("en") ) {
			Properties props = new Properties();
			props.setProperty("annotators","tokenize, ssplit, pos, lemma");
			props.setProperty("tokenize.whitespace","true");
			parser = new StanfordCoreNLP(props);
		}
		else if( type.equals("ch") ) {
			Properties props = new Properties();
			try {
				String properties_path = System.getProperty("user.dir")+"/src/main/resources/StanfordCoreNLP-chinese.properties";
				props.load( new FileInputStream(properties_path) );
			} catch (IOException e) {
				e.printStackTrace();
			}
		    props.setProperty("annotators","tokenize, ssplit, pos, lemma");
		    parser = new StanfordCoreNLP(props);
		}
		
	}
	private ArrayList<String> getLemmaList(String text){
		ArrayList<String> lemmaList = new ArrayList<String>();
		Annotation document = new Annotation(text);
    	parser.annotate(document);
	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		lemmaList.add(token.lemma());
	    	}
        }
	    return lemmaList;
	}
	public void initLabeledFile(String labeledFileName) {
		boolean isFileExists = FileIOs.isFileExists(labeledFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+labeledFileName);
			return;
		}
		System.err.println("正在初始化已标注数据集: "+labeledFileName);
		parserInit("en");
		aspect_text_list_map = new HashMap<String,ArrayList<String>>();
		List<String> lineList = FileIOs.readFileGetStringList(labeledFileName);
		for(int i=0;i<lineList.size();i++) {
			String inputText = lineList.get(i);
			int first_tab_index = inputText.indexOf("\t");
			String text = inputText.substring(0,first_tab_index);
			String acos_list_string = inputText.substring(first_tab_index+1,inputText.length());
			ArrayList<ACSOUnit> acso_list = getACOSUnitList(acos_list_string);
			HashSet<int[]> aspect_set = new HashSet<int[]>();
			for(ACSOUnit acso:acso_list) {
				aspect_set.add(acso.getAspect());
			}
			ArrayList<String> lemmaList = getLemmaList(text);
			for(int[] aspect:aspect_set) {
				String aspectText = getArrSlice(lemmaList,aspect[0],aspect[1]);
				if(!aspect_text_list_map.containsKey(aspectText)) {
					aspect_text_list_map.put(aspectText, new ArrayList<String>());
				}
				aspect_text_list_map.get(aspectText).add(inputText);
			}
		}	
	}
	
	Pattern acso_pattern = Pattern.compile("(\\d+,\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+,\\d+)");
	private ArrayList<ACSOUnit> getACOSUnitList(String acos_list_string){
		ArrayList<ACSOUnit> acos_list = new ArrayList<ACSOUnit>();
        Matcher matcher = acso_pattern.matcher(acos_list_string);
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
	
	public void checkRisk(String tergetFileName) {
		boolean isFileExists = FileIOs.isFileExists(tergetFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+tergetFileName);
			return;
		}
		System.out.println("正在检查文件: "+tergetFileName);
		List<String> lineList = FileIOs.readFileGetStringList(tergetFileName);
		for(String word:lineList) {
			word = word.trim();
			if( word.length()==0 ) {
				continue;
			}
			ArrayList<String> riskAspectList = new ArrayList<String>();
			for(String aspect:aspect_text_list_map.keySet()) {
				String[] aspect_terms = aspect.split("\\s+");
				for(String aspect_term:aspect_terms) {
					if( aspect_term.equals(word) ) {
						riskAspectList.add(aspect);
						break;
					}
				}
			}
			if( riskAspectList.size()>0 ) {
				System.out.println("Word: "+word);
				for(String riskAspect:riskAspectList) {
					System.out.println("Risk Aspect: "+riskAspect);
					ArrayList<String> text_list = aspect_text_list_map.get(riskAspect);
					for(String text:text_list) {
						System.out.println(text);
					}
				}
				System.out.println();
			}
		}
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
	
	private String getArrSlice(String[] arr,int start,int end) {
		String slice = "";
		for(int i=start;i<end;i++) {
			String unit = arr[i];
			slice += unit+" ";
		}
		return slice.trim();
	}
	
	private String getArrSlice(ArrayList<String> list,int start,int end) {
		String slice = "";
		for(int i=start;i<end;i++) {
			String unit = list.get(i);
			slice += unit+" ";
		}
		return slice.trim();
	}

}
