package evaluator.pointerbench.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.SootMethod;
import soot.jimple.Stmt;

public class QueryInfo {

	private Stmt testStmt;
	private SootMethod method;
	private String testVariable;
	private static final Pattern TAG_REGEX = Pattern.compile("\\{(.*?)\\}");
	private static final Pattern TAG_REGEX2 = Pattern.compile("\\[(.*?)\\]");

	private Map<String, AliasInfo> allocationSites = new HashMap<String, AliasInfo>();


	public void registerTestInfo(Stmt testStmt, SootMethod method,String testVariable,
			String results) {
		this.method = method;
		this.testStmt = testStmt;
		this.testVariable = testVariable
				.substring(1, testVariable.length() - 1);

		final Matcher matcher = TAG_REGEX.matcher(results);
		while (matcher.find()) {
			String aliasInfo = matcher.group(1);
			AliasInfo ai = extractMayInfo(aliasInfo);
			String id = extractId(aliasInfo);
			if (allocationSites.containsKey(id)) {
				Stmt s = allocationSites.get(id).getStmt();
				ai.setStmt(s);
			}
			allocationSites.put(id, ai);
		}
	}

	// Take the first one
	private String extractId(String aliasInfo) {
		String[] parts = aliasInfo.split(",");
		return parts[0].substring(parts[0].indexOf(":") + 1, parts[0].length());
	}

	// Take the second and third one (may information)
	private AliasInfo extractMayInfo(String aliasInfo) {
		final Matcher matcher = TAG_REGEX2.matcher(aliasInfo);
		matcher.find();
		String mayAlias = matcher.group(1);
		matcher.find();
		String notMayAlias = matcher.group(1);
		AliasInfo ai = new AliasInfo(mayAlias, notMayAlias);
		return ai;
	}

	public void registerAllocationSite(Stmt allocSite, String id) {
		AliasInfo ai = new AliasInfo(allocSite);
		if (allocationSites.containsKey(id)) {
			ai = allocationSites.get(id);
			ai.setStmt(allocSite);
		}
		allocationSites.put(id, ai);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Query at stmt " + testStmt + " for variable " + testVariable
				+ "\n");
		for (String s : allocationSites.keySet()) {
			sb.append("\t" + allocationSites.get(s).toString() + "\n");
		}
		return sb.toString();
	}
	
	public Stmt getStmt(){
		return testStmt;
	}
	
	public String getVariable(){
		return testVariable;
	}
	
	public SootMethod getMethod(){
		return method;
	}

	public Map<String, AliasInfo> getAllocationSiteInfo(){
		return allocationSites;
	}

	public int computeExecpectedPointsToSize() {
		int counter = 0;
		for(String key : allocationSites.keySet()){
			if(!key.equals("NULLALLOC"))
				counter++;
		}
		return counter;
	}

	public Set<String> getTruePositives() {
		Set<String> out = new HashSet<>();
		for(String key : allocationSites.keySet()){
			AliasInfo aliasInfo = allocationSites.get(key);
			out.addAll(aliasInfo.getAliases());
		}
		return out;
	}

}
