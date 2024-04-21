package extractor.coreNLPRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;

/**
 * Adapt the rules in English
 */
public class EnglishCoreNLPRules extends CoreNLPRules{
	
	@Override
	public boolean isCopulaNode(IndexedWord node) {
		return node.lemma().toLowerCase().equals("be");
	}
	
	@Override
	public boolean isRightParenthesis(String word) {
		return word.equals(")");
	}
	
	@Override
	public boolean isLeftParenthesis(String word) {
		return word.equals("(");
	}
	
	@Override
	public boolean isAppropriateRelnToPeopleObject(GrammaticalRelation reln) {
		String spec = reln.getSpecific();
		if( spec!=null && (spec.equals("to") || spec.equals("for") || spec.equals("as")) ) {
			return false;
		}
		return true;
	}
	
	private String[] puncts = new String[] {",",":",".","HYPH"};
	@Override
	public boolean isPunctuation(IndexedWord node) {
		return isHave(node.tag(),puncts);
	}
	
	public String simplifyTag(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag;
	}
	
	@Override
	public boolean isAdv(String tag) {
		return simplifyTag(tag).equals("RB");
	}
	
	@Override
    public boolean isAdj(String tag) {
    	return simplifyTag(tag).equals("JJ");
	}
    
	@Override
    public boolean isVerb(String tag) {
		return simplifyTag(tag).equals("VB");
	}
    
	@Override
    public boolean isNoun(String tag) {
		return simplifyTag(tag).equals("NN");
	}
	
	@Override
	public boolean isPronoun(String tag) {
		return tag.indexOf("PRP")!=-1;
	}
	
	@Override
	public boolean isDeterminer(String tag) {
		return tag.indexOf("DT")!=-1;
	}
    
    private String[] verbCollocateWthAdvcl = {"make","find"};
    public boolean isVerbCollocateWthAdvcl(IndexedWord node) {
		return isHave(node.lemma(),verbCollocateWthAdvcl);
	}
	
	private String[] corefDT = {"this","that","those","these"};
	public boolean isCorefDT(IndexedWord node) {
		return isHave(node.lemma(),corefDT);
	}
  	
    //宾语部分：
	//是否是直接宾语关系
	@Override
	public boolean isDirectObjReln(String reln) {
		return reln.indexOf("obj")!=-1;
	}
	//是否是合规的间接宾语关系
	@Override
	//是否是合规的间接宾语关系
  	public boolean isLegalInDirectObjReln(String reln) {
  		if( reln.indexOf("obl")!=-1 ) {
			// "as"是有争议的
			//if( reln.indexOf("as")!=-1 ) {
			//	return false;
			//}
			if( reln.indexOf("tmod")!=-1 ) {
				return false;
			}
			if( reln.indexOf("than")!=-1 ) {
				return false;
			}
			if( reln.indexOf("despite")!=-1 ) {
				return false;
			}
			if( reln.indexOf("after")!=-1 ) {
				return false;
			}
			if( reln.indexOf("over")!=-1 ) {
				return false;
			}
			if( reln.indexOf("npmod")!=-1 ) {
				return false;
			}
			if( reln.indexOf("within")!=-1 ) {
				return false;
			}
			if( reln.indexOf("except")!=-1 ) {
				return false;
			}
			if( reln.indexOf("through")!=-1 ) {
				return false;
			}
			if( reln.indexOf("from")!=-1 ) {
				return false;
			}
			else {
				return true;
			}
		}
  		return false;
  	}
    
	//并列词相关：
	@Override
	public boolean isAndReln(String reln) {
  		return reln.equals("conj:and");
  	}
	
	//compound相关：
	@Override
	public boolean isCompoundReln(String reln) {
  		return reln.equals("compound");
  	}
	
	//appos相关：
	@Override
	public boolean isApposReln(String reln) {
  		return reln.equals("appos");
  	}
	
	private String punctuationRegex = "^[,.!?;:'\"()\\-\\[\\]{}+*/=%<>√^]+$";
	@Override
	public boolean isAllPunctuation(IndexedWord node) {
		return node.lemma().matches(punctuationRegex);
	}

}
