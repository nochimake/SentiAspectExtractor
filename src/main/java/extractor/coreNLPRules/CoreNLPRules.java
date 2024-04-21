package extractor.coreNLPRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;

public abstract class CoreNLPRules {
	
	//词性分析相关内容：
	public abstract boolean isCopulaNode(IndexedWord node);
	public abstract boolean isRightParenthesis(String word);
	public abstract boolean isLeftParenthesis(String word);
	public abstract boolean isPunctuation(IndexedWord node);
	public abstract boolean isAllPunctuation(IndexedWord node);
	
	public abstract boolean isAdv(String tag);
	public abstract boolean isAdj(String tag);
	public abstract boolean isVerb(String tag);
	public abstract boolean isNoun(String tag);
	public abstract boolean isPronoun(String tag);
	public abstract boolean isDeterminer(String tag);
	
	public boolean isNumber(String tag) {
		return tag.equals("CD");
	}
	public boolean isAdv(IndexedWord node) {
		return isAdv(node.tag());
	}
    public boolean isAdj(IndexedWord node) {
		return isAdj(node.tag());
	}
    public boolean isVerb(IndexedWord node) {
		return isVerb(node.tag());
	}
    public boolean isNoun(IndexedWord node) {
		return isNoun(node.tag());
	}
    public boolean isPronoun(IndexedWord node) {
		return isPronoun(node.tag());
	}
    public boolean isDeterminer(IndexedWord node) {
		return isDeterminer(node.tag());
	}
    public boolean isNumber(IndexedWord node) {
		return isNumber(node.tag());
	}
    public boolean isHave(String word,String[] arr) {
		for(int i=0;i<arr.length;i++) {
			if(word.equals(arr[i])) {
			   return true;
		    }
		}
	    return false;
    }
	
	
	//句法依存分析相关内容：
	public abstract boolean isVerbCollocateWthAdvcl(IndexedWord node);
	
	public abstract boolean isCorefDT(IndexedWord node);
	
	public abstract boolean isAppropriateRelnToPeopleObject(GrammaticalRelation reln);
	
	public boolean isModReln(String reln) {
		String relnSimplified = reln;
		int indexOfColon = reln.indexOf(":");
		if( indexOfColon!=-1 ) {
			relnSimplified = reln.substring(0,indexOfColon);
		}
		return relnSimplified.indexOf("mod")!=-1;
	}
	
	public boolean isInDescendants(SemanticGraph graph,IndexedWord gov,IndexedWord dep) {
    	if( gov==null ) {
    		return false;
    	}
    	// 若 gov 和 dep 为一个节点，会返回这个节点本身：
    	List<IndexedWord> nodeInPath = graph.getShortestDirectedPathNodes(gov,dep);
    	if( nodeInPath==null || nodeInPath.size()==0 ) {
    		return false;
    	}
    	return true;
    }
	
	public boolean isInACL(SemanticGraph graph,IndexedWord gov,IndexedWord dep) {
    	if( gov==null ) {
    		return false;
    	}
    	// 若 gov 和 dep 为一个节点，会返回这个节点本身：
    	List<IndexedWord> nodeInPath = graph.getShortestDirectedPathNodes(gov,dep);
    	if( nodeInPath==null || nodeInPath.size()==0 ) {
    		return false;
    	}
    	for(int i=0;i<nodeInPath.size()-1;i++) {
    		IndexedWord govv = nodeInPath.get(i);
    		IndexedWord depp = nodeInPath.get(i+1);
    		String reln = graph.getEdge(govv, depp).getRelation().toString();
    		if( reln.startsWith("acl") ) {
    			return false;
    		}
    	}
    	return true;
    }
	
	//主语部分：
	/**
	 * 查找距离节点node最近的主语
	 *
	 * @param node 需要查找主语的节点
	 * @param graph node所在的SemanticGraph
	 * @return ImmutablePair.of(subj,nodeToRoot) subj:找到的主语，nodeToRoot:主语在图中的父节点
	 */
	public Pair<IndexedWord,IndexedWord> getNearestSubjGovPair(IndexedWord node,SemanticGraph graph) {
  		ArrayList<IndexedWord> nodeListToRoot = new ArrayList<IndexedWord>();
  		nodeListToRoot.add(node);
  		nodeListToRoot.addAll(graph.getPathToRoot(node));
  		for(int i=0;i<nodeListToRoot.size();i++) {
  			IndexedWord nodeToRoot = nodeListToRoot.get(i);
  			IndexedWord nodeGov = i+1<nodeListToRoot.size() ? nodeListToRoot.get(i+1) : null ;
  			String reln = nodeGov!=null ? graph.getEdge(nodeGov,nodeToRoot).getRelation().toString() : "" ;
  			if( isSubjReln(reln) ) { 
				return ImmutablePair.of(null,null);
  			}
  			Set<IndexedWord> childSet = graph.getChildren(nodeToRoot);
  			ArrayList<IndexedWord> subjList = new ArrayList<IndexedWord>();
  			for(IndexedWord child:childSet) {
  				reln = graph.getEdge(nodeToRoot,child).getRelation().toString();
  				if( isSubjReln(reln) ) {
  					subjList.add(child);
  				}
  			}
  			if( subjList.size()!=0 ) {
  				IndexedWord subj = selectFromSameLevelSubj(node,subjList);
  				Pair<IndexedWord,IndexedWord> subjGovPair = ImmutablePair.of(subj,nodeToRoot);
  				return subjGovPair;
  			}
  		}
		return ImmutablePair.of(null,null);
  	}
	
	public IndexedWord getNearestSubj(IndexedWord node,SemanticGraph graph) {
		Pair<IndexedWord,IndexedWord> subjGovPair = getNearestSubjGovPair(node,graph);
		return subjGovPair.getLeft();
  	}
	
	/**
	 * 查找节点node的直接主语
	 */
  	public IndexedWord getImmediateSubj(IndexedWord node,SemanticGraph graph) {
  		ArrayList<IndexedWord> subjList = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
  		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isSubjReln(reln) ) {
				subjList.add(child);
			}
		}
  		if( subjList.size()!=0 ) {
  			IndexedWord subj = selectFromSameLevelSubj(node,subjList);
			return subj;	
		}else {
			return null;
		}
  	}
  	
    /**
	 * 从同一深度的 ArrayList 中找到合适的 subj
	 */
  	public IndexedWord selectFromSameLevelSubj(IndexedWord node, ArrayList<IndexedWord> subjList) {
  		IndexedWord subj = null;
  		if( subjList.size()==1 ) {
			subj = subjList.get(0);
		}
		else {
			IndexedWord nearestNonPRPSubj = null;
			IndexedWord nearestPRPSubj = null;
			for(int j=0;j<subjList.size();j++) {
				IndexedWord candidateSubj = subjList.get(j);
				if( isPronoun(candidateSubj) ) {
					if( nearestPRPSubj==null ) {
						nearestPRPSubj = candidateSubj;
					}else if( getDistanceBetweenNodes(nearestPRPSubj,node)>getDistanceBetweenNodes(candidateSubj,node) ){
						nearestPRPSubj = candidateSubj;
					}
				}else {
					if( nearestNonPRPSubj==null ) {
						nearestNonPRPSubj = candidateSubj;
					}else if( getDistanceBetweenNodes(nearestNonPRPSubj,node)>getDistanceBetweenNodes(candidateSubj,node) ){
						nearestNonPRPSubj = candidateSubj;
					}
				}
			}
			if( nearestNonPRPSubj==null ) {
				subj = nearestPRPSubj;
			}
			else {
				subj = nearestNonPRPSubj;
			}
		}	
  		return subj;	
  	}
  	
  	private int getDistanceBetweenNodes(IndexedWord node1,IndexedWord node2) {
		return Math.abs(node1.index()-node2.index());
	}
  	
  	public boolean isSubjReln(String reln) {
  		return reln.indexOf("subj")!=-1;
  	}
  	
  	//宾语部分：
    //是否是直接宾语关系
  	public abstract boolean isDirectObjReln(String reln);
    //是否是合规的间接宾语关系
  	public abstract boolean isLegalInDirectObjReln(String reln);	
    //是否为合理的宾语关系(直接间接皆可)
  	public boolean isLegalObjReln(String reln) {
  		boolean isDirectObjReln = isDirectObjReln(reln);
  		boolean isLegalInDirectObjReln = isLegalInDirectObjReln(reln);
  		return isDirectObjReln || isLegalInDirectObjReln;
  	}	
    //找到最近的宾语
  	public Set<IndexedWord> getNearestObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> nearestObjSet = new HashSet<IndexedWord>();
  		Queue<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        boolean hasFound = false;
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	Set<IndexedWord> childSet = graph.getChildren(node);
        	for(IndexedWord child:childSet) {
  				String reln = graph.getEdge(node,child).getRelation().toString();
  				if( isLegalObjReln(reln) ) {
  					nearestObjSet.add(child);
  					hasFound = true;
  				}
  				queue.add(child);
  			}
        	if( hasFound ) {
        		break;
        	}
        }
  		return nearestObjSet;
  	} 	
    //找到直系关系中的所有宾语
  	public Set<IndexedWord> getImmediateObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> objNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isLegalObjReln(reln) ) {
				objNodeSet.add(child);
			}
		}
		return objNodeSet;
  	}	
    //找到直系关系中的所有直接宾语
  	public Set<IndexedWord> getImmediateDirectObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> objNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isDirectObjReln(reln) ) {
				objNodeSet.add(child);
			}
		}
		return objNodeSet;
  	}
   
    //找到 并列词汇
  	public abstract boolean isAndReln(String reln);
  	public ArrayList<IndexedWord> getImmediateAndNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> andNodeSet = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isAndReln(reln) && !andNodeSet.contains(child) ) {
				andNodeSet.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( isAndReln(reln) && !andNodeSet.contains(gov) ) {
				andNodeSet.add(gov);
			}
		}
		return andNodeSet;
	}
	public ArrayList<IndexedWord> getAllAndNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> andNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeAndNodeList = getImmediateAndNode(node,graph);
        	for(IndexedWord immeAndNode:immeAndNodeList) {
  				if( !andNodeList.contains(immeAndNode) ) {
  					andNodeList.add(immeAndNode);
  					queue.add(immeAndNode);
  				}
  			}
        }
        return andNodeList;
	}
	
	//找到 compound
	public abstract boolean isCompoundReln(String reln);
	public ArrayList<IndexedWord> getImmediateCompoundNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> compoundNodeList = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isCompoundReln(reln) && !compoundNodeList.contains(child) ) {
				compoundNodeList.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( isCompoundReln(reln) && !compoundNodeList.contains(gov) ) {
				compoundNodeList.add(gov);
			}
		}
		return compoundNodeList;
	}
	public ArrayList<IndexedWord> getAllCompoundNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> compoundNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeCompoundNodeList = getImmediateCompoundNode(node,graph);
        	for(IndexedWord immeCompoundNode:immeCompoundNodeList) {
  				if( !compoundNodeList.contains(immeCompoundNode) ) {
  					compoundNodeList.add(immeCompoundNode);
  					queue.add(immeCompoundNode);
  				}
  			}
        }
        return compoundNodeList;
	}
	
	//找到 appos
	public abstract boolean isApposReln(String reln);
	public ArrayList<IndexedWord> getImmediateApposNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> apposNodeSet = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isApposReln(reln) && !apposNodeSet.contains(child) ) {
				apposNodeSet.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( isApposReln(reln) && !apposNodeSet.contains(gov) ) {
				apposNodeSet.add(gov);
			}
		}
		return apposNodeSet;
	}
	public ArrayList<IndexedWord> getAllApposNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> apposNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeApposNodeList = getImmediateApposNode(node,graph);
        	for(IndexedWord immeApposNode:immeApposNodeList) {
  				if( !apposNodeList.contains(immeApposNode) ) {
  					apposNodeList.add(immeApposNode);
  					queue.add(immeApposNode);
  				}
  			}
        }
        return apposNodeList;
	}
	
	
	//选区分析相关内容：
	//从叶节点出发能找到的最小的名词选区：
	public Tree getNearestNounTree(Tree root,Tree leaf) {
		//i=0 为自身单词；i=1为词性
		Tree grandFather = leaf.ancestor(2,root);
		String label = grandFather.value();
		if( label.startsWith("N") ) {
			return grandFather;
		}
		else {
			return null;
		}
	}
	
	//从叶节点出发能找到的最大的名词选区：
	public Tree getMaxNounTree(Tree root,Tree leaf) {
		Tree maxNounTree = null;
		int depth = root.depth();
		//i=0 为自身单词；i=1为词性
		for(int i=2;i<depth;i++) {
			Tree ancestor = leaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( label.startsWith("N") ) {
				maxNounTree = ancestor;
			}else {
				break;
			}
		}
		return maxNounTree;
	}
	
	//返回 node1 和 node2 的最低共同祖先；
	public Tree getLowestCommonAncestor(Tree root,Tree node1,Tree node2) {
		int depth = root.depth();
		for(int i=0;i<depth;i++) {
			Tree ancestor = node1.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.contains(node2) ) {
				return ancestor;
			}
		}
		return null;
	}
	
	//返回共同祖先下的最大名词团块
	public Tree getMaxNounTreeUnderLCA(Tree root,Tree apleaf,Tree opleaf) {
		Tree lowestCommonAncestor = getLowestCommonAncestor(root,apleaf,opleaf);
		Tree maxNounTree = null;
		int depth = root.depth();
		//i=0 为自身单词；i=1为词性
		for(int i=2;i<depth;i++) {
			Tree ancestor = apleaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( label.startsWith("N") ) {
				maxNounTree = ancestor;
			}else {
				break;
			}
			if( ancestor.equals(lowestCommonAncestor) ) {
				break;
			}
		}
		return maxNounTree;
	}
	
	// 判断节点是否在某种类型团块中
	public boolean isNodeInCentainPhrase(Tree node,Tree root,String phraseName) {
		boolean isNodeInCentainPhrase = false;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = node.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( nodeString.equals(phraseName) ) {
				isNodeInCentainPhrase = true;
				break;
			}
		}
		return isNodeInCentainPhrase;
	}	
	//是否位于ADVP中
	public boolean isNodeInADVP(Tree node,Tree root) {
		String phraseName = "ADVP";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
	//是否位于ADVP中
	public boolean isNodeInADJP(Tree node,Tree root) {
		String phraseName = "ADJP";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
	//是否位于从句中
	public boolean isNodeInSBAR(Tree node,Tree root) {
		String phraseName = "S";
		return isNodeInCentainPhrase(node,root,phraseName);
	}	
	//是否位于PRN中
	public boolean isNodeInPRN(Tree node,Tree root) {
		String phraseName = "PRN";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
		
	//Tree node 和 IndexedWord node 指代同一个对象
	public boolean isReferToSame(Tree tree,IndexedWord node) {
		String treeLabel = tree.label().toString();
		int lastHYPHIndex = treeLabel.lastIndexOf("-");
		String word = treeLabel.substring(0,lastHYPHIndex);
		int index = Integer.parseInt( treeLabel.substring(lastHYPHIndex+1,treeLabel.length()) );
		return word.equals(node.word()) && index==node.index();
	}

}
