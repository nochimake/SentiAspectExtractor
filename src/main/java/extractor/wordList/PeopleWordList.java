package extractor.wordList;

import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;
import extractor.FileIOs;

public class PeopleWordList {
	private HashSet<String> peopleWordSet;
	
	public void initPeopleWordList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("File does not exist! "+path);
			return;
		}
		peopleWordSet = new HashSet<String>();
		List<String> wordList = FileIOs.readFileGetStringList(path);
		for(String word:wordList) {
			if(word.length()==0) {
				continue;
			}
			peopleWordSet.add(word);
		}
	}
	
	public boolean isPeopleWord(String word) {
		return peopleWordSet.contains(word);
	}
	
	public boolean isPeopleNode(IndexedWord node) {
		if( node==null ) {
			return false;
		}
		boolean isPeopleLemma = isPeopleWord(node.lemma().toLowerCase());
		if( isPeopleLemma ) {
			return true;
		}
		boolean isPeopleWord = isPeopleWord(node.word().toLowerCase());
		if( isPeopleWord ) {
			return true;
		}
		String NER = node.ner()==null ? "":node.ner();
		if( NER.equals("PERSON") ) {
			return true;
		}
		return false;
	}

}
