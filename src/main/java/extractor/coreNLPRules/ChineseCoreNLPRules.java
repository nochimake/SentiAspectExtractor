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
 * Adapt the rules in Chinese
 */
public class ChineseCoreNLPRules extends CoreNLPRules{
	
	@Override
	public boolean isCopulaNode(IndexedWord node) {
		return node.tag().equals("VC");
	}
	
	@Override
	public boolean isRightParenthesis(String word) {
		return word.equals("）");
	}
	
	@Override
	public boolean isLeftParenthesis(String word) {
		return word.equals("（");
	}
	
	@Override
	public boolean isAppropriateRelnToPeopleObject(GrammaticalRelation reln) {
		String spec = reln.getSpecific();
		return spec==null;
	}
	
	@Override
	public boolean isPunctuation(IndexedWord node) {
		return node.tag().equals("PU");
	}
	
	@Override
	public boolean isAdv(String tag) {
		return tag.equals("AD");
	}
	
	String[] adj_pos_arr = new String[]{"JJ","VA"};
	@Override
    public boolean isAdj(String tag) {
    	return isHave(tag,adj_pos_arr);
	}
	
	String[] verb_pos_arr = new String[]{"VV","VC","VE"};
	@Override
    public boolean isVerb(String tag) {
		return isHave(tag,verb_pos_arr);
	}
    
	String[] noun_pos_arr = new String[]{"NN","NR","NT"};
	@Override
    public boolean isNoun(String tag) {
		return isHave(tag,noun_pos_arr);
	}
	
	@Override
	public boolean isPronoun(String tag) {
		return tag.equals("PN");
	}
	
	@Override
	public boolean isDeterminer(String tag) {
		return tag.equals("DT");
	}
    
	// 中文依存关系分析，无“advcl”
    private String[] verbCollocateWthAdvcl = {};
    @Override
    public boolean isVerbCollocateWthAdvcl(IndexedWord node) {
		return isHave(node.lemma(),verbCollocateWthAdvcl);
	}
    @Override
	public boolean isCorefDT(IndexedWord node) {
		return node.lemma().startsWith("这") || node.lemma().startsWith("那");
	}
	
	//宾语相关：
  	//是否是直接宾语关系
	@Override
  	public boolean isDirectObjReln(String reln) {
  		return reln.equals("dobj");
  	}
    //是否是合规的间接宾语关系
	@Override
  	public boolean isLegalInDirectObjReln(String reln) {
  		return false;
  	}
    
    //并列词相关：
	@Override
  	public boolean isAndReln(String reln) {
  		return reln.equals("conj");
  	}
	
	//compound相关：
	@Override
	public boolean isCompoundReln(String reln) {
  		return reln.equals("compound:nn");
  	}
	
	//appos相关：
	@Override
	public boolean isApposReln(String reln) {
  		return reln.equals("prnmod");
  	}

	private String punctuationRegex = "^[，。？！；：“”‘’（）【】《》—…＋－＝÷×＜＞≤≥≠√]+$";
	@Override
	public boolean isAllPunctuation(IndexedWord node) {
		return node.lemma().matches(punctuationRegex);
	}

}
