package extractor.wordList;

import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;
import extractor.FileIOs;

public class ImplicitAspectWordList {
	private HashSet<String> implicitAspectWordSet;
	public void initImplicitAspectWordList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("File does not exist! "+path);
			return;
		}
		implicitAspectWordSet = new HashSet<String>();
		List<String> wordList = FileIOs.readFileGetStringList(path);
		for(String word:wordList) {
			if(word.length()==0) {
				continue;
			}
			implicitAspectWordSet.add(word);
		}
	}
	
	public boolean isWordInImplicitAspectWordSet(String word) {
		return implicitAspectWordSet.contains(word);
	}
	
	public boolean isImplicitWordByRegular(String word) {
		if( word.endsWith("thing") || word.endsWith("things") ) {
			return true;
		}
		else if( word.startsWith("one") ) {
			return true;
		}
		return false;
	}
	
	public boolean isImplicitNode(IndexedWord node) {
		if( node==null ) {
			return false;
		}
		String lemma = node.lemma().toLowerCase();
		boolean isImplicitWord = isWordInImplicitAspectWordSet(lemma);
		if( isImplicitWord ) {
			return true;
		}
		String word = node.word().toLowerCase();
		boolean iImplicitWordByRegular = isImplicitWordByRegular(word);
		return iImplicitWordByRegular;
	}

}
