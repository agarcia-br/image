package utils;

import java.util.HashMap;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.*;

class Param {

	
	private char    pLetter; 
	private String  pFullName; 
	private String  pMeaning;
	private String  pArgumentName;
	private Integer pArgumentOrder;
	
	public final char getpLetter() {
		return pLetter;
	}
	public final String getpFullName() {
		return pFullName;
	}
	public final String getpMeaning() {
		return pMeaning;
	}
	public final String getpArgumentName() {
		return pArgumentName;
	}
	public final Integer getpArgumentOrder() {
		return pArgumentOrder;
	}
	public Param(char letter, String fullName, String argumentName, String meaning, Integer argumentOrder) {
		pLetter=letter; 
		pFullName=fullName; 
		pMeaning=meaning;
		pArgumentName=argumentName;
		pArgumentOrder=argumentOrder;
	}
}


public class Parameters {
	private Map<Character,Param> paramsByLetter = new HashMap<Character,Param>();
	private Map<String,Param>    paramsByName   = new HashMap<String,Param>();
	private Map<Integer,Param>    paramsByCount = new HashMap<Integer,Param>();
	private Map<String,Character>  abreviations = new HashMap<String,Character>();
	private Map<Character,String>           argValuesByLetter = new HashMap<Character,String>();
	private Set<Character>                  usedParameters    = new HashSet<Character>();
	
	private void setArgByLetter(Character letter, String value) {
		argValuesByLetter.put(letter,value);
	}
	
	
	private int argumentCount = 0;
	private int paramAndArgsCount=0;
	private boolean hasOptionalArgument = false; 
	private void addParamInternal(char letter, String fullName,  String argumentName, String meaning, Integer argumentOrder) {
		Param p = new Param( letter,  fullName,  argumentName, meaning,   argumentOrder);
		paramAndArgsCount++;
		paramsByCount.put(paramAndArgsCount,p);
		paramsByLetter.put(p.getpLetter(),p);	
		paramsByName.put(p.getpFullName(),p);
		abreviations.put(p.getpFullName(),p.getpLetter());
	}	
	
	private int mandatoryCount = 0;
	private String mandatoryLetters = "";

	private StringBuilder madatoryArgs() {
		StringBuilder ret = new StringBuilder();
		for (int i=1;i<=paramAndArgsCount;++i) {
		    if (paramsByCount.containsKey(i)) {
		    	Param p= paramsByCount.get(i);
		    	if (null==p.getpFullName()) {
		    		ret.append('<').append(p.getpArgumentName()).append("> ");
		    	}
		    }
		}
		return ret;
	}

	static private final int FIRST_COL_LEN=8;	
	static private final int SECOND_COL_LEN=20;	
	static private final int THIRD_COL_LEN=20;	

	private StringBuilder formatLine(String s1, String s2, String s3, String s4) {
		StringBuilder ret = new StringBuilder();
		s1 = StringUtils.rightPad(null==s1?"":s1,FIRST_COL_LEN,' ');
		s2 = StringUtils.rightPad(null==s2?"":s2,SECOND_COL_LEN,' ');
		s3 = StringUtils.rightPad(null==s3?"":s3,THIRD_COL_LEN,' ');
		s4 = null==s4?"":s4;
		return ret.append(s1).append(s2).append(s3).append(s4);
	}

	private int error;
	private int argsRead;
	private int optCount;
	private String argLetters;
	private void useParameter(Character c) {
		if (paramsByLetter.containsKey(c)) { 
		    if (usedParameters.contains(c)) {
		    	error |= ERROR_PARAMETER_DOUBLEUSE;
		    } else {	
		    	usedParameters.add(c);
		    	if (null!= paramsByLetter.get(c).getpArgumentName()) {
		    		++optCount;
		    		argLetters = argLetters +c;		    		
		    	}
		    }
		} else {
			error |= ERROR_UNKONW_PARAMETER;
		}
	}
	
	/* **************** *
	 * PUBLIC INTERFACE *
	 * **************** */
	
	/* PUBLIC CONSTANTS */
	static public final int NO_ERROR=0;
	static public final int ERROR_UNKONW_PARAMETER=1;
	static public final int ERROR_PARAMETER_DOUBLEUSE=2;
	static public final int ERROR_TOO_FEW_ARGUMENTS=4;
	static public final int ERROR_TOO_MANY_ARGUMENTS=8;
	static public final int ERROR_INCONSISTENT_PARAMETERS=16;
	static public final int ERROR_EXCEPTION=1024;
	/* PUBLIC CONSTRUCTOR */
	public Parameters() {
		
	}
	/* PUBLIC METHODS */
	public void addParamWithArgument(char letter, String fullName,  String argumentName, String meaning) {
		hasOptionalArgument = true;
		argumentCount++;
		addParamInternal( letter,  fullName,  argumentName, meaning, argumentCount);
	}	
	public void addParam(char letter, String fullName, String meaning) {
		addParamInternal( letter,  fullName, null, meaning, null);
	}
	public void addMandatoryArgument(char letter, String argumentName, String meaning) throws ParametersException {
		if (hasOptionalArgument) {
			throw new ParametersException("Add mandatory arguments first");
		}
		mandatoryCount++;
		argumentCount++;
		mandatoryLetters = mandatoryLetters+letter;
		addParamInternal( letter,  null, argumentName, meaning,   argumentCount);
	}
	public void printErrorMessage(PrintStream ps) {
		if ((error&ERROR_UNKONW_PARAMETER)>0) ps.println("ERROR_UNKONW_PARAMETER");
		if ((error&ERROR_PARAMETER_DOUBLEUSE)>0) ps.println("ERROR_PARAMETER_DOUBLEUSE");
		if ((error&ERROR_TOO_FEW_ARGUMENTS)>0) ps.println("ERROR_TOO_FEW_ARGUMENTS");
		if ((error&ERROR_TOO_MANY_ARGUMENTS)>0) ps.println("ERROR_TOO_MANY_ARGUMENTS");
		if ((error&ERROR_EXCEPTION)>0) ps.println("ERROR_EXCEPTION");
	}
	public boolean getFlag(char letter) {
		return usedParameters.contains(letter);
	}
	public String getArg(Character letter) {
		return argValuesByLetter.get(letter);
	}

	public void printUsageMessage(PrintStream ps, String jarname) {
		ps.println("");
		ps.println("Usage:");
		ps.println("\t java -jar "+jarname+" [options] "+madatoryArgs()+" [opt_arguments]");
		ps.println("");
		ps.println(formatLine("Option","Long Option","Argument","Meaning (default)"));
		for (int i=1;i<=paramAndArgsCount;++i) {
		    if (paramsByCount.containsKey(i)) {
		    	Param p= paramsByCount.get(i);
		    	if (null!=p.getpFullName()) {
		    		ps.println(formatLine("-"+p.getpLetter(),
		    				"--"+p.getpFullName(),
		    				p.getpArgumentName()==null?null:"<"+p.getpArgumentName()+">",
		    				p.getpMeaning()));
		    	}
		    }
		}
	}
	public int readCommandLine(String []args) {
		argLetters = mandatoryLetters;
		error = NO_ERROR;
		argsRead =0;
		optCount =0;
		for (String s:args) {
			if (s.startsWith("--")) {
				Character a = abreviations.get(s=s.substring(2));
				if (null==a) {
					error |= ERROR_UNKONW_PARAMETER;
				} else {
			    	useParameter(a);
				}
			} else if (s.startsWith("-")) { 
				for (char c : s.substring(1).toCharArray()) { 
			    	useParameter(c);
				}
			} else {
				++argsRead;
				if (argsRead>argLetters.length()) {
					error |= ERROR_TOO_MANY_ARGUMENTS;
				} else {
					setArgByLetter(argLetters.charAt(argsRead-1),s);
				}
			}
		}
		if (argsRead < mandatoryCount+optCount) error |= ERROR_TOO_FEW_ARGUMENTS;
		if (argsRead > mandatoryCount+optCount) error |= ERROR_TOO_MANY_ARGUMENTS;
		return error;
	}

}
