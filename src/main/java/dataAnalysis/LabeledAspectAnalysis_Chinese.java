package dataAnalysis;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import extractor.FileIOs;


/**
 * We use this class to analyze Chinese annotated text, 
 * such as identifying its verb aspect and analyzing common paths from opinion to aspect.
 */
public class LabeledAspectAnalysis_Chinese {
	public StanfordCoreNLP parser;
	public String labeledFileName;
	public static void main(String[] args) {
		LabeledAspectAnalysis_Chinese ana = new LabeledAspectAnalysis_Chinese();
		String resourcePath = System.getProperty("user.dir") + "/src/main/resources/";
		String labeledFileName = resourcePath+"phone_as_test.tsv";
		String opinionFileName = resourcePath+"phone_input.txt";
//		ana.verbAspectSearch(labeledFileName);
		ana.analysisDepPathBetweenAspectOpinion(labeledFileName,opinionFileName);
	}
	
	public void parserInit() {
		Properties props = new Properties();
		try {
			String properties_path = System.getProperty("user.dir")+"/src/main/resources/StanfordCoreNLP-chinese.properties";
			props.load( new FileInputStream(properties_path) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	    props.setProperty("annotators","tokenize, ssplit, pos, lemma, parse");
	    parser = new StanfordCoreNLP(props);
	}
	
	/**
     * Analyze common paths from opinion to aspect
     */
	public void analysisDepPathBetweenAspectOpinion(String labeledFileName,String opinionFileName) {
		boolean isFileExists = FileIOs.isFileExists(labeledFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+labeledFileName);
			return;
		}
		isFileExists = FileIOs.isFileExists(opinionFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+opinionFileName);
			return;
		}
		System.err.println("Start analysisDepPathBetweenAspectOpinion(): ");
		System.err.println("labeledFileName: "+labeledFileName);
		System.err.println("opinionFileName: "+opinionFileName);
		parserInit();
		ArrayList<int[]> opinion_list = new ArrayList<int[]>();
		List<String> lineList = FileIOs.readFileGetStringList(opinionFileName);
		for(int i=0;i<lineList.size();i++) {
			String inputText = lineList.get(i);
			int first_tab_index = inputText.indexOf("\t");
			String text = inputText.substring(0,first_tab_index);
			String opinion_string = inputText.substring(first_tab_index+1,inputText.length());
			int[] opinion = stringToIntArray(opinion_string);
			opinion_list.add(opinion);
		}
		lineList = FileIOs.readFileGetStringList(labeledFileName);
		HashMap<String,ArrayList<String>> path_text_map = new HashMap<String,ArrayList<String>>();
		for(int i=0;i<lineList.size();i++) {
			String inputText = lineList.get(i);
			int first_tab_index = inputText.indexOf("\t");
			String text = inputText.substring(0,first_tab_index);
			HashMap<Integer,List<Integer>> oriIndexTokenIndexMap = new HashMap<Integer,List<Integer>>();
			HashMap<Integer,List<Integer>> tokenIndexOriIndexMap = new HashMap<Integer,List<Integer>>();
			chTextTokenMapping(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
			String acos_list_string = inputText.substring(first_tab_index+1,inputText.length());
			ArrayList<ACSOUnit> acso_list = getACOSUnitList(acos_list_string);
			HashSet<int[]> aspect_set = new HashSet<int[]>();
			for(ACSOUnit acso:acso_list) {
				int[] aspect_token = transInputIndexArrToTokenIndexArr(acso.getAspect(),oriIndexTokenIndexMap);
				aspect_set.add( aspect_token );
			}
			int[] opinion = opinion_list.get(i);
			int[] opinion_token = transInputIndexArrToTokenIndexArr(opinion,oriIndexTokenIndexMap);
			ArrayList< DependencyPath > short_path_list =getShortPathFromOpinionToAspect(text,opinion_token,aspect_set);
			for(DependencyPath dep_path:short_path_list) {
				String path_string = dep_path.getPath().toString();
				if( !path_text_map.containsKey(path_string) ) {
					path_text_map.put(path_string, new ArrayList<String>());
				}
				path_text_map.get(path_string).add(text+"\t"+dep_path.getSource()+"\t"+dep_path.getSourceNode().toString()+"\t"+dep_path.getTarget());
			}
		}
		

        List<Map.Entry<String, ArrayList<String>>> entryList = new ArrayList<Map.Entry<String, ArrayList<String>>>(path_text_map.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, ArrayList<String>>>() {
            @Override
            public int compare(Map.Entry<String, ArrayList<String>> entry1, Map.Entry<String, ArrayList<String>> entry2) {
                int size1 = entry1.getValue().size();
                int size2 = entry2.getValue().size();
                return Integer.compare(size2,size1);
            }
        });

        // 输出排序后的结果
        for (Map.Entry<String, ArrayList<String>> entry : entryList) {
            System.out.println(entry.getKey()+": "+entry.getValue().size());
            for(String text: entry.getValue()) {
            	System.out.println(text);
            }
            System.out.println();
        }
	}
	
	public ArrayList< DependencyPath > getShortPathFromOpinionToAspect(String text, int[] opinion_token_index,HashSet<int[]> aspect_set) {
		ArrayList< DependencyPath > short_path_list = new ArrayList< DependencyPath >();
		Annotation document = new Annotation(text);
		parser.annotate(document);
		List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
		ArrayList<SemanticGraph> graphList = new ArrayList<SemanticGraph>();
		ArrayList<IndexedWord> nodeList = new ArrayList<IndexedWord>();
	    for (CoreMap sentence : sentences) {
	    	SemanticGraph graph =sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
	    	graphList.add(graph);
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		nodeList.add( graph.getNodeByIndex(token.index()) );
	    	}
	    }
	    String opinion = getText(opinion_token_index,nodeList);
	    for(int[] aspect_index:aspect_set) {
	    	String aspect = getText(aspect_index,nodeList);
			List<SemanticGraphEdge> short_edges = null ;
			IndexedWord short_edges_source = null;
			for(int i=aspect_index[0];i<=aspect_index[1];i++) {
				IndexedWord aspect_token = nodeList.get(i);
				for(SemanticGraph graph:graphList) {
		    		if( graph.containsVertex(aspect_token) ) {
		    			for(int j=opinion_token_index[0];j<=opinion_token_index[1];j++) {
		    		    	IndexedWord opinion_token = nodeList.get(j);
		    		    	List<SemanticGraphEdge> edges = graph.getShortestUndirectedPathEdges(opinion_token, aspect_token);
	    					if( short_edges==null || edges.size()<short_edges.size() ) {
	    						short_edges = edges;
	    						short_edges_source = opinion_token;
	    					}
		    		    }
		    			break;
		    		}
		    	}
			}
			ArrayList<Dependency> short_edges_transed = transform(short_edges,short_edges_source);
			short_path_list.add(new DependencyPath(opinion,short_edges_source,aspect,short_edges_transed));
		}
	    return short_path_list;
	}
	
	private String getText(int[] tokenInde,ArrayList<IndexedWord> nodeList) {
		String text = "";
		for(int i=tokenInde[0];i<=tokenInde[1];i++) {
			text += nodeList.get(i).word()+" ";
		}
		return text.trim();
	}
	
	public ArrayList<Dependency> transform(List<SemanticGraphEdge> edges,IndexedWord source){
		ArrayList<Dependency> dep_list = new ArrayList<Dependency>();
		for(SemanticGraphEdge edge : edges) {
			String reln = edge.getRelation().toString();
			Dependency dep = null;
			if( edge.getDependent().equals(source) ) {
				dep = new Dependency(1,reln);
				source = edge.getGovernor();
			}else {
				dep = new Dependency(-1,reln);
				source = edge.getDependent();
			}
			dep_list.add(dep);
		}
		return dep_list;
	}
	
	String[] verb_pos_arr = new String[]{"VV","VC","VE"};
    public boolean isVerb(String tag) {
		return isHave(tag,verb_pos_arr);
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
			chTextTokenMapping(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
			for(int[] aspect:aspect_set) {
				int[] token_index = transInputIndexArrToTokenIndexArr(aspect,oriIndexTokenIndexMap);
				if( token_index[0]==token_index[1] && isVerb(token_list.get(token_index[0]).tag()) ) {
					String key = token_list.get(token_index[0]).originalText();
					if( !verbAspect_text_map.containsKey(key) ) {
						verbAspect_text_map.put(key, new ArrayList<String>());
					}
					verbAspect_text_map.get(key).add(inputText);
				}
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
	
	private void chTextTokenMapping(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
	    int token_index = 0;
		Annotation document = new Annotation(text);
		parser.annotate(document);
		List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
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
	}
	
	Pattern ch_acso_pattern = Pattern.compile("(\\d+,\\d+)\\s+(\\d+)");
	private ArrayList<ACSOUnit> getACOSUnitList(String acos_list_string){
		ArrayList<ACSOUnit> acos_list = new ArrayList<ACSOUnit>();
        Matcher matcher = ch_acso_pattern.matcher(acos_list_string);
        while (matcher.find()) {
            String aspect_string = matcher.group(1); 
            String sentiment_string = matcher.group(2);
            int[] aspect = stringToIntArray(aspect_string);
            int sentiment = Integer.parseInt(sentiment_string);
            ACSOUnit acos = new ACSOUnit(aspect,sentiment);
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
	
	public boolean isHave(String word,String[] arr) {
		for(int i=0;i<arr.length;i++) {
			if(word.equals(arr[i])) {
			   return true;
		    }
		}
	    return false;
    }
	
	private class DependencyPath{
		String source;
		IndexedWord sourceNode;
		String target;
		ArrayList<Dependency> path;
		public DependencyPath(String source, IndexedWord sourceNode, String target, ArrayList<Dependency> path) {
			super();
			this.source = source;
			this.sourceNode = sourceNode;
			this.target = target;
			this.path = path;
		}
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public String getTarget() {
			return target;
		}
		public void setTarget(String target) {
			this.target = target;
		}
		public ArrayList<Dependency> getPath() {
			return path;
		}
		public void setPath(ArrayList<Dependency> path) {
			this.path = path;
		}
		public IndexedWord getSourceNode() {
			return sourceNode;
		}
		public void setSourceNode(IndexedWord sourceNode) {
			this.sourceNode = sourceNode;
		}
	}
	
	private class Dependency{
		int depDirection; //1:向上游查找；-1:向下游查找
		String reln;
		public Dependency(int depDirection, String reln) {
			super();
			this.depDirection = depDirection;
			this.reln = reln;
		}
		public int getDepDirection() {
			return depDirection;
		}
		public void setDepDirection(int depDirection) {
			this.depDirection = depDirection;
		}
		public String getReln() {
			return reln;
		}
		public void setReln(String reln) {
			this.reln = reln;
		}
		public String toString() {
			if( depDirection==1 ) {
				return " <--("+reln+")-- ";
			}else if( depDirection==-1 ) {
				return " --("+reln+")--> ";
			}else {
				return "  ";
			}
		}
	}

}
