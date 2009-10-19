package de.swkk.metadata.archivedb;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

import de.swkk.metadata.GSACallNumber;

public class ArchiveDatabaseRecord extends LinkedHashMap<String, String> implements Comparable<ArchiveDatabaseRecord> {

	public GSACallNumber getCallNumber() {
		if (containsKey("bestandnr") && containsKey("signatur")) {
			return new GSACallNumber(String.format("%s/%s", get("bestandnr"), get("signatur")));
		}

		return null;
	}

	public int getId() {
		return Integer.parseInt(get("num"));
	}

	public Integer getIdentNum() {
		Assert.isTrue(containsKey("ident"));
		return Integer.parseInt(get("ident"));
	}

	public void dump(PrintStream printStream) {
		for (Map.Entry<String, String> field : this.entrySet()) {
			printStream.printf("%s: %s\n", field.getKey(), field.getValue());
		}
	}

	@Override
	public int compareTo(ArchiveDatabaseRecord o) {
		return getCallNumber().compareTo(o.getCallNumber());
	}
}
