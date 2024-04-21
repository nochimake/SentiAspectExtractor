package extractor.textExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.TypesafeMap;
import extractor.AnalysisOptions;
import extractor.Aspect;
import extractor.Opinion;
import extractor.coreNLPRules.CoreNLPRules;
import extractor.textParser.TextParser;
import extractor.wordList.ImplicitAspectWordList;
import extractor.wordList.PeopleWordList;
import extractor.wordList.TimeWordList;
import extractor.wordList.VerbAspectList;

/**
 * The main processes for extracting aspects are in this class
 */
public abstract class TextExtractor {
	public AnalysisOptions OPT;
	public TextParser textParser;
	public CoreNLPRules NLPRule;
	public PeopleWordList peopleWordList;
	public TimeWordList timeWordList;
	public VerbAspectList verbAspectList;
	public ImplicitAspectWordList implicitAspectWordList;
	
	public void init() {
		textParser.setOption(OPT);
		textParser.spliterInit();
		textParser.parserInit();
		peopleWordList = new PeopleWordList();
		peopleWordList.initPeopleWordList(OPT.peopleWordListPath);
		timeWordList = new TimeWordList();
		timeWordList.initTimeWordList(OPT.timeWordListPath);
		verbAspectList = new VerbAspectList();
		verbAspectList.initVerbAspectList(OPT.verbAspectWordListPath);
		implicitAspectWordList = new ImplicitAspectWordList();
		implicitAspectWordList.initImplicitAspectWordList(OPT.implicitAspectWordListPath);
		
	}
	
	public void setOption(AnalysisOptions opt) {
		this.OPT = opt;
	}
	
	private HashMap<Integer,List<Integer>> oriIndexTokenIndexMap;
	private HashMap<Integer,List<Integer>> tokenIndexOriIndexMap;
	
	public abstract String preprocessInputText(String text,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap);
	
	private int[] transInputIndexArrToTokenIndexArr(int[] inputIndexOPArr,HashMap<Integer,List<Integer>> oriIndexTokenIndexMap) {
		int startOriIndex = inputIndexOPArr[0];
		int endOriIndex = inputIndexOPArr[1]-1;
		int[] tokenIndexOPArr = new int[2];
		tokenIndexOPArr[0] = oriIndexTokenIndexMap.get(startOriIndex).get(0);
		tokenIndexOPArr[1] = oriIndexTokenIndexMap.get(endOriIndex).get( oriIndexTokenIndexMap.get(endOriIndex).size()-1 );
		return tokenIndexOPArr;
	}
	
	private int[] transTokenIndexArrToOriIndexArr(int[] tokenIndexArr,HashMap<Integer,List<Integer>> tokenIndexOriIndexMap) {
		int[] oriIndexOPArr = new int[2];
		oriIndexOPArr[0] = -1;
		oriIndexOPArr[1] = -1;
		int startTokenIndex = tokenIndexArr[0];
		int endTokenIndex = tokenIndexArr[1];
		if( startTokenIndex!=-1 && endTokenIndex!=-1 ) {
			oriIndexOPArr[0] = tokenIndexOriIndexMap.get(startTokenIndex).get(0);
			oriIndexOPArr[1] = tokenIndexOriIndexMap.get(endTokenIndex).get( tokenIndexOriIndexMap.get(endTokenIndex).size()-1 )+1;
		}
		return oriIndexOPArr;
	}
	
	public void parseText(String text) {
		HashMap<Integer,List<Integer>> oriIndexTokenIndexMap = new HashMap<Integer,List<Integer>>();
		HashMap<Integer,List<Integer>> tokenIndexOriIndexMap = new HashMap<Integer,List<Integer>>();
		String text_preprocessed = preprocessInputText(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
		this.oriIndexTokenIndexMap = oriIndexTokenIndexMap;
		this.tokenIndexOriIndexMap = tokenIndexOriIndexMap;
		textParser.initText(text_preprocessed);
	}	
	
	protected abstract IndexedWord selectCoreOpinionNode(ArrayList<IndexedWord> opinion);
	
	private void structureOpinion(Opinion op) {
		int[] tokenIndex = transInputIndexArrToTokenIndexArr(op.getOriIndexArr(),this.oriIndexTokenIndexMap);
		ArrayList<IndexedWord> opinionNodeList = new ArrayList<IndexedWord>();
    	int startTokenIndex = tokenIndex[0];
    	int endTokenIndex = tokenIndex[1];
    	for(int i=startTokenIndex;i<=endTokenIndex;i++) {
			IndexedWord opinionTermNode = textParser.getNodeByIndex(i);
			opinionNodeList.add(opinionTermNode);
		}
    	IndexedWord coreOpinionNode = selectCoreOpinionNode(opinionNodeList);
    	op.setCoreOpinionNode(coreOpinionNode);
    	op.setOpinionNodeList(opinionNodeList);
	}
	
	public ArrayList<Aspect> extractForOpinion(Opinion op) {
    	if( !textParser.isTextInit() ) {
    		System.err.println(" extractForOpinion() 之前需要初始化文本!");
    		return null;
    	}
    	ArrayList< Aspect > aspectList = new ArrayList< Aspect >();
    	structureOpinion(op);
    	if( op.isLegal() ) {
    		if( op.isPotentialAspect() ) {
    			Aspect ap = new Aspect(op.getCoreOpinionNode(),"[R-0] SHAP Potential Aspect (SPA)");
    			ap.setAspectNodeList( op.getOpinionNodeList() );
    			extendAspectAndAddToList(op,ap,aspectList); 
    		}
    		else {
    			//对所有词性通用的规则:
    			extractForGeneral_SubjectRule(op,aspectList);
    			extractForGeneral_PredicativeRule(op,aspectList);
    			extractForGeneral_SubordinateRule(op,aspectList);
    			//根据词性分类讨论:
        		String coreOpinionNodetag = op.getCoreOpinionNodeTag();
        		if( NLPRule.isAdj(coreOpinionNodetag) ) {
        			extractForAdjOpinion(op,aspectList);
        		}
            	else if( NLPRule.isVerb(coreOpinionNodetag) ) {
            		extractForVerbOpinion(op,aspectList);
            	}
            	else if( NLPRule.isNoun(coreOpinionNodetag) ) {
            		extractForNounOpinion(op,aspectList);
            	}
            	else if( NLPRule.isAdv(coreOpinionNodetag) ) {
            		extractForAdvOpinion(op,aspectList);
            	}
            	else {
            		extractForOtherOpinion(op,aspectList);
            	}
    		}
    	}
    	for(Aspect ap:aspectList) {
    		int[] tokenIndexArr = getTokenIndexForAspect(ap);
    		ap.setTokenIndexArr(tokenIndexArr);
    		int[] inputIndexArr = transTokenIndexArrToOriIndexArr(tokenIndexArr,this.tokenIndexOriIndexMap);
    		ap.setOriIndexArr(inputIndexArr);
    	}
		return aspectList;
    }
	
	private int[] getTokenIndexForAspect(Aspect ap) {
		int[] tokenIndexArr = new int[2];
		int[] aspectBeginPosScopeArr = ap.getAspectBeginPosScopeArr();
		tokenIndexArr[0] = textParser.getNodeIndexByBeginPos(aspectBeginPosScopeArr[0]);
		tokenIndexArr[1] = textParser.getNodeIndexByBeginPos(aspectBeginPosScopeArr[1]);
		return tokenIndexArr;
	}
	
	protected void extractForGeneral_SubjectRule(Opinion op,ArrayList<Aspect> aspectList) {
		IndexedWord coreNode = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(coreNode);
		//关于主语的探讨主语：
    	Pair<IndexedWord,IndexedWord> subjGovPair = NLPRule.getNearestSubjGovPair(coreNode,graph);
    	IndexedWord nearestSubj = subjGovPair.getLeft();
    	IndexedWord nearestSubjGov = subjGovPair.getRight();
    	//若存在主语
    	if( nearestSubj!=null ) {
    		//若主语为人
    		if( peopleWordList.isPeopleNode(nearestSubj) ) {
    			
    		}
    		//若主语为疑问词
    		//e.g. "a {home pc laptop}-[ASPECT] that works very {well}-[OPINION] ."
    		else if( nearestSubj.tag().equals("WDT") ) {
    			if( nearestSubjGov!=null ) {
    				int tokenIndex = textParser.getNodeIndexByNode(nearestSubj);
    				IndexedWord prevOfWDT = textParser.getNodeByIndex(tokenIndex-1);
    				if( prevOfWDT!=null && NLPRule.isNoun(prevOfWDT.tag()) ) {
    					int rightmostBoundary = prevOfWDT.index();
    					String reasonForSelection = "[R-8] opinion 位于 acl 中，取 acl 的修饰对象为 aspect (WH-Word)";
        				Aspect ap = new Aspect(prevOfWDT,reasonForSelection);
        				ap.setRightMostBoundary(rightmostBoundary);
        				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    				}
    			}
    		}
    		//若主语为动词：
    		else if( NLPRule.isVerb(nearestSubj.tag()) ) {
    			Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(nearestSubj,graph);
	  			if( objNodeSet.size()!=0 ) {
	  			    //e.g. configuring the {printer}-[ASPECT] was a little tricky but {not bad}-[OPINION] .
	  				String reasonForSelection = "[R-3] opinion 的主语为动词，且存在宾语，则取宾语";
	  				Set<IndexedWord> setFiltered = filterObjNodeSet(graph,nearestSubj,objNodeSet);
	  				for(IndexedWord objNode:setFiltered) {
	  					Aspect ap = new Aspect(objNode,reasonForSelection);
	  					if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
	  				}
	  			}
	  			else {
	  				//e.g. {set up}-[ASPECT] was {awesome}-[OPINION]
	  				String reasonForSelection = "[R-4] opinion 的主语为动词，且不存在宾语，则取动词";
  					Aspect ap = new Aspect(nearestSubj,reasonForSelection);
  					if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
	  			}	
	    	}
    		//若主语为形容词 
    		// e.g. "most of my {android apps}-[ASPECT] have worked {well}-[OPINION] ."
    		else if( NLPRule.isAdj(nearestSubj.tag()) ) {
    			IndexedWord nmod = null;
    			Set<IndexedWord> childSet = graph.getChildren(nearestSubj);
    			for(IndexedWord child:childSet) {
    				String reln = graph.getEdge(nearestSubj,child).getRelation().getShortName();
    				if( reln.equals("nmod") ) {
    					nmod = child;
    				}
    			}
    			if( isLegalSubj(nmod) ) {
    				String reasonForSelection = "[R-2] opinion 的主语为形容词，则取形容词的修饰对象";
    				Aspect ap = new Aspect(nmod,reasonForSelection);
    				if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
    			}
    		}
    		// 名词主语
    		// e.g. the {keyboard}-[ASPECT] is {stiff}-[OPINION]
    		else if ( isLegalSubj(nearestSubj) ) {
    			String reasonForSelection = "[R-1] opinion 的合法词性主语";
    			Aspect ap = new Aspect(nearestSubj,reasonForSelection);
    			if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
    		}
    	}
	}
	
	protected void extractForGeneral_PredicativeRule(Opinion op,ArrayList<Aspect> aspectList) {
		IndexedWord coreNode = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(coreNode);
    	ArrayList<IndexedWord> nodeList = textParser.getNodeList();
    	for(int i=0;i<nodeList.size();i++) {
    		IndexedWord node = nodeList.get(i);
    		//找到Be-Verb,处理 "主-系-表结构" 
    		if( NLPRule.isCopulaNode(node) ) {
    			if( !graph.containsVertex(node) ) {
    				continue;
    			} 
    			IndexedWord BEGov = graph.getParent(node);
    			//Be-Verb 为根节点, "主-系-表结构"中 系语 为 从句 ：
    			if( BEGov==null ) {
    				IndexedWord subj = NLPRule.getImmediateSubj(node, graph);
    				IndexedWord ccompOfVerb = null;
    	    		Set<IndexedWord> childSet = graph.getChildren(node);
    	    		for(IndexedWord child:childSet) {
    	    			String reln = graph.getEdge(node,child).getRelation().toString();
    	    			if( reln.equals("ccomp") ) {
    	    				ccompOfVerb = child;
    	    			}
    	    		}
    	    		//opinion 位于"主-系-表"的主语中：取从句的主语为aspect:
    	    		//e.g. "my only {complaint}-[OPINION] is that the {mouse keypad}-[ASPECT] is a little big ."
    	    		boolean isInSubjDescendants = NLPRule.isInDescendants(graph,subj,coreNode);
    	    		if( isInSubjDescendants && ccompOfVerb!=null ) {
    	    			IndexedWord clauseSubj = NLPRule.getImmediateSubj(ccompOfVerb,graph);
    	    			if( clauseSubj!=null && isLegalSubj(clauseSubj)) {
    	    				String reasonForSelection = "[R-5] opinion 位于主语中，且表语为从句，则取表语从句主语为aspect";
        					Aspect ap = new Aspect(clauseSubj,reasonForSelection);
        					if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
    	    			}
    	    		}
    	    	}else {
    				String reln = graph.getEdge(BEGov, node).getRelation().toString();
    				//"主-系-表结构"中 系语 为 短语 ：
    				if( reln.equals("cop") ) {
    					IndexedWord subj = NLPRule.getImmediateSubj(BEGov, graph);
        	    		//opinion 位于主-系-表，的主语中：取系语短语为aspect
        	    		//e.g. "- biggest {disappointment}-[OPINION] is the {track pad}-[ASPECT] ."
        	    		boolean isInDescendants = NLPRule.isInDescendants(graph,subj,coreNode);
        	    		if( isInDescendants && NLPRule.isNoun(BEGov.tag()) ) {
        	    			String reasonForSelection = "[R-6] opinion 位于主语中，且表语为短语，则取短语为aspect";
        	    			Aspect ap = new Aspect(BEGov,reasonForSelection);
        	    			if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,ap,aspectList); };
        	    		}
    				}
    			}
    		}
    	}
	}
	
	protected void extractForGeneral_SubordinateRule(Opinion op,ArrayList<Aspect> aspectList) {
		IndexedWord coreNode = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(coreNode);
    	ArrayList<IndexedWord> nodeList = textParser.getNodeList();
    	//关于以从句修饰的探索：
    	//case 1:修饰从句以括号与主体连接
    	//e.g." backlit and solid {keyboard}-[ASPECT] ( {not flimsy or cheap}-[OPINION] )"
    	IndexedWord nodeLeadBracket = null;
    	IndexedWord startOPNode = op.getOpinionStartNode();
    	int startOPNodeIndex = textParser.getNodeIndexByNode(startOPNode);
    	for(int i=startOPNodeIndex-1;i>0;i--) {
    		IndexedWord node = nodeList.get(i);
    		if( NLPRule.isRightParenthesis(node.word()) ) {
    			break;
    		}
    		else if(  NLPRule.isLeftParenthesis(node.word()) ) {
    			nodeLeadBracket = nodeList.get(i-1);
    		}
    	}
    	if( nodeLeadBracket!=null && isLegalByTag(nodeLeadBracket) ) {
    		int rightmostBoundary = nodeLeadBracket.index();
    		String reasonForSelection = "[R-7] opinion 位于括号中，取括号前一个单词为aspect";
    		Aspect ap = new Aspect(nodeLeadBracket,reasonForSelection);
    		ap.setRightMostBoundary(rightmostBoundary);
    		if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    	}
    	//case 2: 通过"acl"关系查找从句：
    	for (SemanticGraphEdge edge : graph.edgeIterable()) {
    		String reln = edge.getRelation().toString();
    		if( reln.startsWith("acl") ) {
    			IndexedWord gov = edge.getGovernor();
    			IndexedWord dep = edge.getDependent();
    			boolean isInACL = NLPRule.isInACL(graph,dep,coreNode);
    			if( isInACL && isLegalByTag(gov) ) {
    				if( peopleWordList.isPeopleNode(gov) ) {
    					continue;
    				}else {
    					IndexedWord clauseModObj = gov;
    					// 取acl的修饰对象为aspect    
    					// e.g. "a {home pc laptop}-[ASPECT] that works very {well}-[OPINION] ."
    					int rightmostBoundary = clauseModObj.index();
        				String reasonForSelection = "[R-8] opinion 位于 acl 中，取 acl 的修饰对象为aspect";
        				Aspect ap = new Aspect(clauseModObj,reasonForSelection);
        				ap.setRightMostBoundary(rightmostBoundary);
        				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    					
    					
        				// 若acl的修饰对象为主系表中的表语，则再取主语为aspect	
        				// e.g. "the {ports}-[ASPECT] were another thing that i was really {excited}-[OPINION] to see "
        				IndexedWord subj = NLPRule.getImmediateSubj(clauseModObj,graph);
        				IndexedWord cop = null;
        				Set<IndexedWord> childOfClauseModObj = graph.getChildren(clauseModObj);
        				for(IndexedWord child:childOfClauseModObj) {
        					reln = graph.getEdge(clauseModObj, child).getRelation().toString();
        					if( reln.equals("cop") ) {
        						cop = child;
        					}
        				}
        				if( cop!=null && subj!=null ) {
        					reasonForSelection = "[R-9] opinion 位于 acl 中，且 acl 的修饰对象为表语，则取主语为aspect";
                			Aspect subjAp = new Aspect(subj,reasonForSelection);
                			if( OPT.isUseSubjectPredicativeRule ) { extendAspectAndAddToList(op,subjAp,aspectList); }
        				}
        			}
    			}
    		}
    	}
	}
	
	protected void extractForAdjOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord adj = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(adj);
    	
    	//探讨修饰对象：
    	//case 1: 向祖先节点寻找
    	boolean hasFindStandardModObject = false;
    	Set<IndexedWord> modObjectSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> govSet = graph.getParents(adj);
    	if( govSet!=null && govSet.size()!=0 ) {
    		for(IndexedWord gov:govSet) {
    			String reln = graph.getEdge(gov,adj).getRelation().toString();
    			if( NLPRule.isModReln(reln) ) {
    				modObjectSet.add(gov);
    			}
        	}
    	}
    	if( modObjectSet.size()!=0 ) {
    		for(IndexedWord modObject:modObjectSet) {
    			if( NLPRule.isNoun(modObject.tag()) ) {
    				hasFindStandardModObject = true;
    				// 直接修饰对象:
    				// e.g. "a {great}-[OPINION] {computer}-[ASPECT]"
    				boolean isInFaultLoca = modObject.index()<adj.index() && textParser.havePartitionInBetween(modObject, adj);
    				boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || !isInFaultLoca;
    				if( isInReasonableLoca ) {
    					String reasonForSelection = "[R-10] adj_opinion 的修饰对象(直接修饰对象)";
            			Aspect ap = new Aspect(modObject,reasonForSelection,0);
            			if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList) ;}
    				}
    				// 直接修饰对象的所属名词:
    				// e.g. "a {laptop}-[ASPECT] at a {reasonable}-[OPINION] price"
    				IndexedWord mainNoun = null;
    				Set<IndexedWord> modObjGovSet = graph.getParents(modObject);
    				for(IndexedWord modObjGov:modObjGovSet) {
    					String reln = graph.getEdge(modObjGov,modObject).getRelation().getShortName();
    					String spec = graph.getEdge(modObjGov,modObject).getRelation().getSpecific();
    					if( reln.equals("nmod") && ( spec.equals("at") || spec.equals("with") || spec.equals("in") )) {
    						mainNoun = modObjGov;
    					}
    				}
    				if( mainNoun!=null && NLPRule.isNoun(mainNoun) ) {
    					String reasonForSelection = "[R-11] adj_opinion 直接修饰对象的所属名词";
            			Aspect ap = new Aspect(mainNoun,reasonForSelection,0);
            			if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    				}
    			}
    			// 间接修饰对象:
				// e.g. "{nice}-[OPINION] sized {keyboard}-[ASPECT] "
    			else if( NLPRule.isAdj(modObject) ) {
    				Set<IndexedWord> govOfModObject = graph.getParents(modObject);
    				for(IndexedWord govOfMod:govOfModObject) {
    					String reln = graph.getEdge(govOfMod,modObject).getRelation().toString();
    					boolean reasonableLocaDistribution = govOfMod.index()>adj.index();
    					boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
    					if( NLPRule.isModReln(reln) && NLPRule.isNoun(govOfMod) && isInReasonableLoca ) {
    						hasFindStandardModObject = true;
    	    				String reasonForSelection = "[R-12] adj_opinion 的间接修饰对象";
    	        			Aspect ap = new Aspect(govOfMod,reasonForSelection,0);
    	        			if( govOfMod.index()>modObject.index() && govOfMod.index()>adj.index() ) {
    							int leftMostBoundary = modObject.index()+1;
    							ap.setLeftMostBoundary(leftMostBoundary);
    						}
    	        			if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    	    			}
    				}
    			}
    		}
    	}
    	//case 2: 隐性修饰对象, 向后一个单位寻找
    	// e.g. terrible product and {worse}-[OPINION] {customer service}-[ASPECT] - - do not buy
    	if( !hasFindStandardModObject ) {
			int beginPos = adj.beginPosition();
			int tokenIndex = textParser.getNodeIndexByBeginPos(beginPos);
			IndexedWord nextNode = textParser.getNodeByIndex(tokenIndex+1);
			String reasonForSelection = "[R-10] adj_opinion 的修饰对象(隐性修饰对象)";
			if( nextNode!=null && NLPRule.isNoun(nextNode)) {
				Aspect ap = new Aspect(nextNode,reasonForSelection,0);
				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
			}
        }
    	
    	//宾语部分：
    	//case 1:根据使役结构寻找宾语：
    	// e.g. "it will take years of bad programming to make this {chromebook}-[ASPECT] as {slow}-[OPINION] as my last one got ."
    	Set<IndexedWord> advclGovSet = graph.getParentsWithReln(adj, "advcl");
    	for(IndexedWord gov:advclGovSet) {
    		if( NLPRule.isVerbCollocateWthAdvcl(gov) ) {
    			Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(gov,graph);
    			Set<IndexedWord> setFiltered = filterObjNodeSet(graph,gov,objNodeSet);
    			String reasonForSelection = "[R-13] adj_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    			}
    		}
    	}
    	//case 2:直接寻找宾语：
    	//e.g."i'm super {happy}-[OPINION] with this {product}-[ASPECT] ."
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(adj,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(adj,graph);
    		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,adj,objNodeSet);
    		for(IndexedWord objNode:setFiltered) {
				String reasonForSelection = "[R-14] adj_opinion 的宾语(直接)";
				Aspect ap = new Aspect(objNode,reasonForSelection);
				if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
			}
    	}
    	
    	//讨论补语：
    	Set<IndexedWord> ccompNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> advclNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> xcompVerbSet = new HashSet<IndexedWord>();
		Set<IndexedWord> childSet = graph.getChildren(adj);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(adj,child).getRelation().toString();
			//case 1:提取补语动词
			// e.g. very {easy}-[OPINION] to {set up}-[ASPECT] .
			if( reln.equals("xcomp") && NLPRule.isVerb(child.tag()) ) {
				xcompVerbSet.add(child);
				String reasonForSelection = "[R-15] adj_opinion 提取补语动词";
				Aspect ap = new Aspect(child,reasonForSelection);
				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
			}
			else if( reln.equals("advcl") ) {
				advclNodeSet.add(child);
			}
			else if( reln.equals("ccomp") ) {
				ccompNodeSet.add(child);
			}
		}
		
		if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
			//case 2:提取补语从句的主语
    		//e.g.  i'm so {disappointed}-[OPINION] that {it}-[ASPECT] turned out like this 
    		if( advclNodeSet.size()!=0 || ccompNodeSet.size()!=0) {
    			Set<IndexedWord> clauseNodeSet = new HashSet<IndexedWord>();
    			clauseNodeSet.addAll(advclNodeSet);
    			clauseNodeSet.addAll(ccompNodeSet);
    			for(IndexedWord clauseNode:clauseNodeSet) {
    				IndexedWord clauseSubj = NLPRule.getImmediateSubj(clauseNode,graph);
    				if( isLegalSubj(clauseSubj) ) {
	    				String reasonForSelection = "[R-16] adj_opinion 提取从句主语";
	    	    		Aspect ap = new Aspect(clauseSubj,reasonForSelection);
	    	    		if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
	    			}	
    			}
    		}
    		if( xcompVerbSet.size()!=0 ) {
  				for(IndexedWord xcompVerb:xcompVerbSet) {
  					//case 3:讨论补语动词的宾语 
  					// e.g. "i was extremely {hesitant}-[OPINION] to buy a used {macbook pro}-[ASPECT]"
  					Set<IndexedWord> xcompVerbObjNodeSet = NLPRule.getImmediateObj(xcompVerb,graph);
  					Set<IndexedWord> xcompVerbObjSetFiltered = filterObjNodeSet(graph,xcompVerb,xcompVerbObjNodeSet);
  					for(IndexedWord objNode:xcompVerbObjSetFiltered) {
  						String reasonForSelection = "[R-17] adj_opinion 提取补语动词的宾语";
  						Aspect ap = new Aspect(objNode,reasonForSelection);
  						if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
  					}	
  				    //case 4:讨论补语动词的从句主语
  					// e.g. "i'm {happy}-[OPINION] to report that the {keyboard}-[ASPECT] is great."
  		    		IndexedWord xcompVerb_ccomp = null;
  		    		Set<IndexedWord> xcompVerbChildSet = graph.getChildren(xcompVerb);
  		    		for(IndexedWord child:xcompVerbChildSet) {
  		    			String reln = graph.getEdge(xcompVerb,child).getRelation().toString();
  		    			if( reln.equals("ccomp") ) {
  		    				xcompVerb_ccomp = child;
  		    			}
  		    		}
  		    		if( xcompVerb_ccomp!=null ) {
  		    			IndexedWord ccompSubj = NLPRule.getImmediateSubj(xcompVerb_ccomp,graph);
  		    			if( isLegalSubj(ccompSubj) ) {
  		    				String reasonForSelection = "[R-18] adj_opinion 提取补语动词的从句主语";
  		    	    		Aspect ap = new Aspect(ccompSubj,reasonForSelection);
  		    	    		if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
  		    			}
  		    		}
  				}
  			}
    	}
    }
	
	protected void extractForVerbOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord verb = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(verb);
    	String reasonForSelection;
		//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	
    	//讨论修饰对象:
    	//提取verb的修饰对象
    	// e.g.  a {non - functioning}-[OPINION] {touchpad}-[ASPECT]
		Set<IndexedWord> govSet = graph.getParents(verb);
    	if( govSet!=null && govSet.size()!=0 ) {
    		for(IndexedWord gov:govSet) {
    			String reln = graph.getEdge(gov,verb).getRelation().toString();
    			if( NLPRule.isModReln(reln) && NLPRule.isNoun(gov) ) {
    				reasonForSelection = "[R-19] verb_opinion 的修饰对象(直接修饰对象)";
    				Aspect ap = new Aspect(gov,reasonForSelection,0);
    				if( OPT.isUseModifierRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    			}
        	}
    	}
    	
    	//讨论宾语:
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(verb,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		//case 1: 显性的宾语 
    		// e.g  i will {recommend}-[OPINION] this {ssd}-[ASPECT]
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(verb,graph);
        	Set<IndexedWord> setFiltered = filterObjNodeSet(graph,verb,objNodeSet);
			for(IndexedWord objNode:setFiltered) {
				reasonForSelection = "[R-20] verb_opinion 的宾语(显性)";
				Aspect ap = new Aspect(objNode,reasonForSelection);
				if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
			}	
    		//case 2: 隐性的宾语
			// e.g   i {like}-[OPINION] the {laptop}-[ASPECT] feel and look to it .
    		int beginPos = verb.beginPosition();
    		int tokenIndex = textParser.getNodeIndexByBeginPos(beginPos);
    		IndexedWord nextNode = textParser.getNodeByIndex(tokenIndex+1);
    		reasonForSelection = "[R-20] verb_opinion 的宾语(隐性)";
    		if( nextNode!=null ) {
    			if( NLPRule.isNoun(nextNode.tag()) ) {
    				Aspect ap = new Aspect(nextNode,reasonForSelection);
    				if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    			}
    			else if( nextNode.tag().equals("DT") ) {
    				IndexedWord referentialObject = null;
    				IndexedWord gov = graph.getParent(nextNode);
    				String reln = gov!=null ? graph.getEdge(gov,nextNode).getRelation().toString() : "";
    				if( reln.equals("det") ) {
    					referentialObject = gov;
    				}
    				if( referentialObject!=null && NLPRule.isNoun(referentialObject) ) {
    					Aspect ap = new Aspect(referentialObject,reasonForSelection);
    					if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    				}
    				else {
    					IndexedWord secondNextNode = textParser.getNodeByIndex(tokenIndex+2);
    					if( NLPRule.isNoun(secondNextNode) ) {
    						Aspect ap = new Aspect(secondNextNode,reasonForSelection);
    						if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    					}
    				}
    			}
    		}
    		
    		//讨论补语：
    		// 提取补语从句的主语：
    		//e.g. i {love}-[OPINION] how slim the {design}-[ASPECT] is 
    		IndexedWord ccomp = null;
    		Set<IndexedWord> childSet = graph.getChildren(verb);
    		for(IndexedWord child:childSet) {
    			String reln = graph.getEdge(verb,child).getRelation().toString();
    			if( reln.equals("ccomp") ) {
    				ccomp = child;
    			}
    		}
    		if( ccomp!=null ) {
    			IndexedWord ccompSubj = NLPRule.getImmediateSubj(ccomp,graph);
    			if( isLegalSubj(ccompSubj) ) {
    				reasonForSelection = "[R-21] verb_opinion 补语从句的主语";
    	    		Aspect ap = new Aspect(ccompSubj,reasonForSelection);
    	    		if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    			}
    		}
    	}
    }
	
	protected void extractForNounOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord noun = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(noun);
    	
    	//讨论修饰对象：
    	String reasonForSelection = "";
    	Set<IndexedWord> nounGovSet = graph.getParents(noun);
    	//case 1: 查找其祖先关系中的修饰对象
    	//e.g." a {premium}-[OPINION] {laptop}-[ASPECT]"
    	for(IndexedWord gov:nounGovSet) {
    		String reln = gov!=null ? graph.getEdge(gov,noun).getRelation().toString() : "";
        	if( NLPRule.isCompoundReln(reln) && NLPRule.isNoun(gov) ) {
        		reasonForSelection = "[R-22] noun_opinion 的修饰对象(祖先)";
    			Aspect ap = new Aspect(gov,reasonForSelection,0);
    			if( OPT.isUseModifierRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    		}
    	}
		//查找其子代关系中的修饰对象:
    	//case 2: nmod    e.g."{disadvantage}-[OPINION] of {amazon seller}-[ASPECT] "
    	//case 3: compound    e.g."i am a big {chromebook}-[ASPECT] {enthusiast}-[OPINION]"
    	Set<IndexedWord> childSet = graph.getChildren(noun);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(noun,child).getRelation().getShortName();
			if( reln.equals("nmod") && isLegalByTag(child) ) {
				reasonForSelection = "[R-24] noun_opinion 的修饰对象(孩子-nmod)";
				Aspect ap = new Aspect(child,reasonForSelection,0);
				if( OPT.isUseModifierRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
			}
			else if( NLPRule.isCompoundReln(reln) && NLPRule.isNoun(child) ) {
				reasonForSelection = "[R-23] noun_opinion 的修饰对象(孩子-compound)";
				Aspect ap = new Aspect(child,reasonForSelection,0);
				if( OPT.isUseModifierRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
	    	}
		}
		
		//讨论宾语：
		//case-1:通过使役结构查找宾语	
		// e.g. "makes this {machine}-[ASPECT] such a {fun}-[OPINION]"
		for(IndexedWord gov:nounGovSet) {
    		String reln = gov!=null ? graph.getEdge(gov,noun).getRelation().toString() : "";
        	if( NLPRule.isLegalObjReln(reln) && NLPRule.isVerbCollocateWthAdvcl(gov) ) {
        		Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(gov,graph);
        		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,gov,objNodeSet);
    			reasonForSelection = "[R-25] noun_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				if( objNode.index()!=noun.index() ) {
    					Aspect ap = new Aspect(objNode,reasonForSelection);
    					if( OPT.isUseModifierRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    				}
    			}
        	}
        }
		//根据主语讨论宾语：
		IndexedWord nearestSubj = NLPRule.getNearestSubj(noun,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		//case-2:直接宾语	
    		//e.g. "so {kudos}-[OPINION] to {acer}-[ASPECT] for the keyboard !"
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(noun,graph);
    		if( objNodeSet.size()!=0 ) {
    			Set<IndexedWord> setFiltered = filterObjNodeSet(graph,noun,objNodeSet);
    			reasonForSelection = "[R-26] noun_opinion 的宾语(直接)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				if( OPT.isUseObjectRule ) {  extendAspectAndAddToList(op,ap,aspectList); }
    			}
    		}
    	}
	}
	
	protected void extractForAdvOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord adv = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(adv);
    	
    	//讨论修饰对象：
    	Set<IndexedWord> advmodObjSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> depObjSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> parentSet = graph.getParents(adv);
    	for(IndexedWord advGov:parentSet) {
    		String reln = graph.getEdge(advGov,adv).getRelation().toString();
			if( NLPRule.isModReln(reln) ) {
				advmodObjSet.add(advGov);
			}
			else if( reln.equals("dep") ) {
				depObjSet.add(advGov);
			}
    	}
    	for(IndexedWord depObj:depObjSet) {
    		boolean havePartitionInBetween = textParser.havePartitionInBetween(depObj,adv);
			boolean reasonableLocaDistribution = !havePartitionInBetween && (depObj.index()<adv.index() || depObj.index()-adv.index()==1);
			boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
			//case 1.1：祖先中的名词修饰对象(dep)
			//e.g. {fast}-[OPINION] {processor}-[ASPECT] .
			if( isInReasonableLoca && NLPRule.isNoun(depObj) ) {
				String reasonForSelection = "[R-27] adv_opinion 的修饰对象(祖先-dep)";
				Aspect ap = new Aspect(depObj,reasonForSelection,0);
				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
			}
    	}
    	for(IndexedWord advmodObj:advmodObjSet) {
    		boolean havePartitionInBetween = textParser.havePartitionInBetween(advmodObj,adv);
			boolean reasonableLocaDistribution = !havePartitionInBetween && (advmodObj.index()<adv.index() || advmodObj.index()-adv.index()==1);
			boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
			//case 1.2：祖先中的名词修饰对象(mod)
			//e.g. not that this machine {boots}-[ASPECT] up {slow}-[OPINION] .
			if( isInReasonableLoca && NLPRule.isNoun(advmodObj) ) {
				String reasonForSelection = "[R-27] adv_opinion 的修饰对象(祖先-mod)";
				Aspect ap = new Aspect(advmodObj,reasonForSelection,0);
				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
			}
			//case 2：动词修饰对像
			//e.g. {navigates}-[ASPECT] {well}-[OPINION] .
			if( NLPRule.isVerb(advmodObj) ) {
    			String reasonForSelection = "[R-28] adv_opinion 的修饰对象(动词)";
    			Aspect ap = new Aspect(advmodObj,reasonForSelection,2);
    			if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    		}
			//case 3：间接修饰对象
			//e.g. a {cheap}-[OPINION] , decently rugged , light weight {notebook}-[ASPECT]
			if( NLPRule.isAdv(advmodObj) || NLPRule.isAdj(advmodObj) || NLPRule.isVerb(advmodObj) ){
				for(IndexedWord gov:graph.getParents(advmodObj)) {
					String reln = graph.getEdge(gov,advmodObj).getRelation().toString();
					if( NLPRule.isModReln(reln) && NLPRule.isNoun(gov) ) {
						String reasonForSelection = "[R-29] adv_opinion 的间接修饰对象";
						Aspect ap = new Aspect(gov,reasonForSelection,0);
						if( gov.index()>advmodObj.index() && gov.index()>adv.index() ) {
							int leftMostBoundary = advmodObj.index()+1;
							ap.setLeftMostBoundary(leftMostBoundary);
						}
						if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
					}
				}
			}
    	}
		
    	//讨论宾语：
		//case 1:通过使役结构，寻找宾语：
    	//e.g. "hdd makes this {laptop}-[ASPECT] very {slow}-[OPINION] ."
    	for(IndexedWord advmodObj:advmodObjSet) {
    		if( NLPRule.isVerbCollocateWthAdvcl(advmodObj) ) {
    			Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(advmodObj,graph);
        		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,advmodObj,objNodeSet);
    			String reasonForSelection = "[R-30] adv_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				if( OPT.isUseModifierRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    			}
    		}
    	}
    	//case 2:提取修饰动词的宾语：
    	//e.g. i {rarely}-[OPINION] use the {keyboard}-[ASPECT] .
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(adv,graph);
    	if( nearestSubj!=null && peopleWordList.isPeopleNode(nearestSubj) ) {
    		for(IndexedWord advmodObj:advmodObjSet) {
				Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(advmodObj,graph);
				if( objNodeSet.size()!=0 ) {
					String reasonForSelection = "[R-31] adv_opinion 的宾语";
					Set<IndexedWord> setFiltered = filterObjNodeSet(graph,advmodObj,objNodeSet);
					for(IndexedWord objNode:setFiltered) {
						Aspect ap = new Aspect(objNode,reasonForSelection,1);
						if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
					}	
				}
			}
    	}
    }
	
	protected void extractForOtherOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord node = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(node);
    	// e.g. {like}-[OPINION] the {flip}-[ASPECT] , this has good build quality .
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
    		if( isLegalByTag(gov) ) {
    			String reasonForSelection = "[R-32] other_opinion 的对象";
    			Aspect ap = new Aspect(gov,reasonForSelection,0);
    			if( OPT.isUseObjectRule ) { extendAspectAndAddToList(op,ap,aspectList); }
    		}
    	}
    }
	
	protected boolean isLegalByTag(IndexedWord node) {
    	return node!=null && (NLPRule.isNoun(node) || NLPRule.isNumber(node) || NLPRule.isDeterminer(node) || NLPRule.isPronoun(node));
    }
    
    protected boolean isLegalObject(IndexedWord node) {
    	return isLegalByTag(node) && !timeWordList.isTimeNode(node);
    }
    
    protected boolean isLegalSubj(IndexedWord node) {
    	return isLegalByTag(node) && !peopleWordList.isPeopleNode(node);
    }
	
	protected Set<IndexedWord> filterObjNodeSet(SemanticGraph graph,IndexedWord gov,Set<IndexedWord> objNodeSet) {
    	Set<IndexedWord> setFiltered = new HashSet<IndexedWord>();
    	for(IndexedWord objNode:objNodeSet) {
    		boolean reasonableLocaDistribution = objNode.index()>gov.index(); //合理的位置：若宾语在附属结构后
    		boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
    		if( isInReasonableLoca ) {
    			if( peopleWordList.isPeopleNode(objNode) ) {
    				if( NLPRule.isAppropriateRelnToPeopleObject( graph.getEdge(gov, objNode).getRelation() ) ) {
    					setFiltered.add(objNode);
    				}
    			}
        		else if( isLegalObject(objNode) ) {
    				setFiltered.add(objNode);
    			}
    		}
		}
    	return setFiltered;
    }
    
    protected void extendAspectAndAddToList(Opinion op,Aspect ap,ArrayList<Aspect> aspectList) {
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return;
    	}
    	boolean isCoreNodeInOpinion = isCoreNodeInOpinion(ap,op);
    	if( !op.isPotentialAspect() && isCoreNodeInOpinion ) {
    		return;
    	}
    	IndexedWord node = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(node);
    	ArrayList<IndexedWord> andNodeSet = NLPRule.getAllAndNode(node,graph);
    	ArrayList<Aspect> conjAspectList = new ArrayList<Aspect>();
    	conjAspectList.add(ap);
    	for(IndexedWord coreNode :andNodeSet) {
    		Aspect conjAspect = new Aspect(coreNode,ap.getReasonForSelection());
    		conjAspectList.add(conjAspect);
    	}
    	for(Aspect conjAspect :conjAspectList) {
    		if( NLPRule.isPronoun(node) || NLPRule.isCorefDT(node) ) {
        		checkCoref(op,conjAspect,aspectList);
        	}
        	else if( NLPRule.isVerb(node) ) {
        		extendVerbCoreAspect(op,conjAspect,aspectList);
        	}
        	else if( NLPRule.isNoun(node) || NLPRule.isNumber(node) || NLPRule.isDeterminer(node) ) {
        		extendNounCoreAspect(op,conjAspect,aspectList);
        	}
        	else {
        		addAspectToListWithoutDup(op,conjAspect,aspectList);
        	}	
    	}
    }
    
    private boolean isCoreNodeInOpinion(Aspect ap,Opinion op) {
    	IndexedWord coreNode = ap.getCoreAspectNode();
    	boolean isTermInOpinion = op.isNodeInOpinion(coreNode);
    	return isTermInOpinion;
    }
    
    private void checkCoref(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	IndexedWord node = ap.getCoreAspectNode();
    	ArrayList< ArrayList<CoreLabel> > corefList = textParser.getCoReferList(node);
    	//指代可以被消解
    	if( corefList!=null && corefList.size()!=0) {
    		for(int i=0;i<corefList.size();i++) {
    			ArrayList<CoreLabel> coref = corefList.get(i);
    			ArrayList<IndexedWord> corefNodeList = new ArrayList<IndexedWord>();
    			for(int j=0;j<coref.size();j++) {
    				CoreLabel corefToken = coref.get(j);
    				int beginPos = corefToken.beginPosition();
    				int corefNodeIndex = textParser.getNodeIndexByBeginPos(beginPos);
    				IndexedWord corefNode = textParser.getNodeByIndex(corefNodeIndex);
    				corefNodeList.add(corefNode);
    			}
    			if( corefNodeList.size()!=0 ) {
    				boolean hasIntersectionWithOpinion = op.hasIntersectionWithOpinion(corefNodeList);
        			if( !hasIntersectionWithOpinion ) {
        				Aspect corefAspect = ap.copyAspect();
        				corefAspect.setAspectNodeList(corefNodeList);
        				addAspectToListWithoutDup(op,corefAspect,aspectList);
        			}
    			}
    		}
    	}else {
    		addAspectToListWithoutDup(op,ap,aspectList);
    	}
    }
    
    private void extendVerbCoreAspect(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	IndexedWord verbCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(verbCore);
    	IndexedWord prtNode = null;
    	Set<IndexedWord> childSet = graph.getChildren(verbCore);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(verbCore,child).getRelation().toString();
			if( reln.indexOf("prt")!=-1 ) {
				prtNode = child;
			}
		}
		if( prtNode!=null ) {
			ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
			int startBeginPos = verbCore.beginPosition();
			int endBeginPos = prtNode.beginPosition();
			int startTokenIndex = textParser.getNodeIndexByBeginPos(startBeginPos);
			int endTokenIndex = textParser.getNodeIndexByBeginPos(endBeginPos);
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
			ap.setAspectNodeList(chunk);
		}
		if( verbAspectList.isVerbAspect(ap.getAspectNodeList()) ){
			addAspectToListWithoutDup(op,ap,aspectList);
		}
	}
    
    private void extendNounCoreAspect(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	//根据指代关系扩展核心
    	extendNounByCoref(op,ap,aspectList);
    	//根据并列及同位语关系扩展核心
    	ArrayList<IndexedWord> extended = new ArrayList<IndexedWord>();
    	if( OPT.coreExtendType==0 ) {
    		extended = extendBySemanticCompound(ap);
    	}
    	else if( OPT.coreExtendType==1  ) {
    		extended = extendNounBySemanticGraph(ap);
    	}
    	else if( OPT.coreExtendType==2 ) {
    		ArrayList<IndexedWord> extendedByConstituency = extendNounByConstituency(op,ap);
        	ArrayList<IndexedWord> extendedBySemanticGraph = extendNounBySemanticGraph(ap);
        	extended = mergeOrderly(extendedByConstituency,extendedBySemanticGraph);
    	}
    	if( extended.size()>0 ) {
    		ap.setAspectNodeList(extended);
    	}
		addAspectToListWithoutDup(op,ap,aspectList);
    }
    
    private void extendNounByCoref(Opinion op,Aspect ap,ArrayList<Aspect> aspectList) {
    	IndexedWord nounCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(nounCore);
    	IndexedWord possNode = null;
    	Set<IndexedWord> childSet = graph.getChildren(nounCore);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(nounCore,child).getRelation().toString();
			String childWord = child.word().toLowerCase();
			if( reln.indexOf("nmod")!=-1 && (childWord.equals("its")||childWord.equals("it")) ){
				possNode = child;
			}
		}
		if( possNode!=null ) {
			String reasonForSelection = ap.getReasonForSelection();
			int aspectType = ap.getAspectType();
			Aspect possAp = new Aspect(possNode,reasonForSelection,aspectType);
			checkCoref(op,possAp,aspectList);
		}
    }
    
    private ArrayList<IndexedWord> extendBySemanticCompound(Aspect ap){
    	IndexedWord core = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(core);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	ArrayList<IndexedWord> compoundNodeList = NLPRule.getAllCompoundNode(core,graph);
		if( compoundNodeList.size()!=0 ) {
			int nounCoreTokenIndex = textParser.getNodeIndexByNode(core);
			int startTokenIndex = nounCoreTokenIndex;
			int endTokenIndex = nounCoreTokenIndex;
			for(IndexedWord compoundNode:compoundNodeList) {
				int modObjectTokenIndex = textParser.getNodeIndexByNode(compoundNode);
				startTokenIndex = Math.min(startTokenIndex, modObjectTokenIndex);
				endTokenIndex = Math.max(endTokenIndex, modObjectTokenIndex);
			}
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
		}
    	return chunk;
    }
    
    private ArrayList<IndexedWord> extendNounBySemanticGraph(Aspect ap){
    	IndexedWord nounCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(nounCore);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	Set<IndexedWord> modNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> childSet = graph.getChildren(nounCore);
    	ArrayList<IndexedWord> compoundNodeList = NLPRule.getAllCompoundNode(nounCore,graph);
    	modNodeSet.addAll(compoundNodeList);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(nounCore,child).getRelation().toString();
			if( reln.indexOf("nmod")!=-1 ) {
				modNodeSet.add(child);
			}else if( NLPRule.isLegalObjReln(reln) ) {
				IndexedWord caseNode = null;
				Set<IndexedWord> grandChildSet = graph.getChildren(child);
				for(IndexedWord grandChild:grandChildSet) {
					reln = graph.getEdge(child,grandChild).getRelation().toString();
					if( reln.equals("case") ) {
						caseNode = grandChild;
					}
				}
				boolean reasonableLocaDistribution = caseNode==null || caseNode.index()==nounCore.index()+1;
				boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
				if( isInReasonableLoca ) {
					modNodeSet.add(child);
				}
			}
		}
		if( modNodeSet.size()!=0 ) {
			int nounCoreTokenIndex = textParser.getNodeIndexByNode(nounCore);
			int startTokenIndex = nounCoreTokenIndex;
			int endTokenIndex = nounCoreTokenIndex;
			for(IndexedWord modNode:modNodeSet) {
				int modObjectTokenIndex = textParser.getNodeIndexByNode(modNode);
				startTokenIndex = Math.min(startTokenIndex, modObjectTokenIndex);
				endTokenIndex = Math.max(endTokenIndex, modObjectTokenIndex);
			}
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
		}
		return chunk;
    }
    
    private ArrayList<IndexedWord> extendNounByConstituency(Opinion op,Aspect ap){
    	IndexedWord apCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(apCore);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	Tree root = textParser.getTreeByNode(apCore);
    	Tree apCoreLeaf = textParser.getTreeLeafByNode(apCore);
    	Tree expansionNode = NLPRule.getMaxNounTree(root, apCoreLeaf);
		if( expansionNode!=null ) {
			List<Tree> leafList = expansionNode.getLeaves();
			Tree startNode = leafList.get(0);
    		Tree endNode = leafList.get(leafList.size()-1);
    		int startIndex = textParser.getTreeNodeIndex(startNode);
    		int endIndex =  textParser.getTreeNodeIndex(endNode);
    		for(int i=startIndex;i<=endIndex;i++) {
    			IndexedWord node = graph.getNodeByIndex(i);
    			if( isLegalByTag(node) ) {
    				chunk.add(node);
    			}
    		}
    	}
		return chunk;
    }
    
    private ArrayList<IndexedWord> mergeOrderly(ArrayList<IndexedWord> arr1,ArrayList<IndexedWord> arr2){
    	ArrayList<IndexedWord> mergered = new ArrayList<IndexedWord>();
    	int m = 0;//arr1的索引
    	int n = 0;//arr2的索引
    	for (int i = 0; i<arr1.size()+arr2.size(); i++) {
            if( m<arr1.size() && n<arr2.size() ){
            	if( arr1.get(m).index()<arr2.get(n).index() ) {
            		mergered.add(arr1.get(m));
            		m++;
            	}
            	else if( arr1.get(m).index()==arr2.get(n).index() ) {
            		mergered.add(arr1.get(m));
            		m++;
            		n++;
            	}
            	else {
            		mergered.add(arr2.get(n));
            		n++;
            	}
            }else if( m<arr1.size() ){
            	mergered.add(arr1.get(m));
        		m++;
            }else if( n<arr2.size() ){
            	mergered.add(arr2.get(n));
        		n++;
            }
        }
    	return mergered;
    }
    
    private void addAspectToListWithoutDup(Opinion op,Aspect ap,ArrayList<Aspect> aspectList) {
    	//修剪 aspect 的范围：
    	trimAspectScope(op,ap);
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return ;
    	}
    	//查看显隐性
		boolean isImplicitWord = isImplicitWord(ap);
		if( isImplicitWord ) {
			return ;
    	}
		//再无重复加入：
		boolean isContain = false;
    	for(int i=0;i<aspectList.size();i++) {
    		Aspect apInList = aspectList.get(i);
    		if( ap.isSubset(apInList) ) {
    			isContain = true;
    			break;
    		}
    	}
    	if( !isContain ) {
    		for(int i=0;i<aspectList.size();i++) {
        		Aspect apInList = aspectList.get(i);
        		if( apInList.isSubset(ap) ) {
        			aspectList.remove(apInList);
        			i--;
        		}
        	}
    		aspectList.add(ap);
    	}
    }
    
    private void trimAspectScope(Opinion op,Aspect ap){
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return;
    	}
    	ArrayList<IndexedWord> trimedApNodeList = new ArrayList<IndexedWord>();
    	ArrayList<IndexedWord> apNodeList = ap.getAspectNodeList();
    	IndexedWord apCoreNode = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(apCoreNode);
    	//修剪一：根据和opinion的相对位置进行修剪：
    	IndexedWord opinionStartNode = op.getOpinionStartNode();
    	IndexedWord opinionEndNode = op.getOpinionEndNode();
    	int startIndex = apNodeList.get(0).index();
		int endIndex =  apNodeList.get(apNodeList.size()-1).index();
		if( !op.isPotentialAspect() && opinionStartNode!=null && opinionEndNode!=null) {
			int startMax = Math.max(startIndex, opinionStartNode.index());
			int endMin = Math.min(endIndex,opinionEndNode.index());
			// opinion 和 aspect 当前范围有交集
			if( endMin>=startMax ) {
				if( opinionEndNode.index()<=apCoreNode.index() ) {
					startIndex = opinionEndNode.index()+1;
				}
				if( opinionStartNode.index()>=apCoreNode.index() ) {
					endIndex = opinionStartNode.index()-1;
				}
			}
		}
		for(int i=startIndex;i<=endIndex;i++) {
			IndexedWord node = graph.getNodeByIndex(i);
			trimedApNodeList.add(node);
		}
		//修剪二：根据前期设置进行修剪
		int leftMostBoundary = ap.getLeftMostBoundary();
		int rightMostBoundary = ap.getRightMostBoundary();
		if( leftMostBoundary!=-1 ) {
			for(int i=0;i<trimedApNodeList.size();i++) {
	    		IndexedWord node = trimedApNodeList.get(i);
	    		if( node.index()<leftMostBoundary ) {
	    			trimedApNodeList.remove(node);
	    			i--;
	    		}
	    	}
		}
		if( rightMostBoundary!=-1 ) {
			for(int i=0;i<trimedApNodeList.size();i++) {
	    		IndexedWord node = trimedApNodeList.get(i);
	    		if( node.index()>rightMostBoundary ) {
	    			trimedApNodeList.remove(node);
	    			i--;
	    		}
	    	}
		}
		//修剪三：根据节点的属性进行修剪
		for(int i=0;i<trimedApNodeList.size();i++) {
    		IndexedWord node = trimedApNodeList.get(i);
    		String ner = node.ner();
    		if( !(NLPRule.isNoun(node) ||NLPRule.isNumber(node))  ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    		else if( (ner!=null && ner.equals("ORDINAL")) || timeWordList.isTimeNode(node) ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    	}
		ap.setAspectNodeList(trimedApNodeList);
    }
    
    private boolean isImplicitWord(Aspect ap) {
    	IndexedWord soleNode = ap.getAspectNodeList().size() == 1 ? ap.getAspectNodeList().get(0) : null;
    	return implicitAspectWordList.isImplicitNode(soleNode);
    }
    
}
