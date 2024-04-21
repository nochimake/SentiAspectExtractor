package extractor.wordList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;
import extractor.FileIOs;

public class VerbAspectList {
	private HashSet<String[]>  VerbAspectSet;
	
	public void initVerbAspectList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("File does not exist! "+path);
			return;
		}
		VerbAspectSet = new HashSet<String[]>();
		List<String> lineList = FileIOs.readFileGetStringList(path);
		for(String line:lineList) {
			if(line.length()==0) {
				continue;
			}
			String[] elems = line.split("\\s+");
			VerbAspectSet.add(elems);
		}
	}
	
	public boolean isWordInVerbAspectSet(String[] arr) {
		for(String[] verbAspect:VerbAspectSet) {
			if( Arrays.equals(verbAspect,arr) ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isVerbAspect(ArrayList<IndexedWord> nodeList) {
		String[] lemmaArr = new String[nodeList.size()];
		for(int i=0;i<nodeList.size();i++) {
			lemmaArr[i] = nodeList.get(i).lemma();
		}
		return isWordInVerbAspectSet(lemmaArr);
	}
}
