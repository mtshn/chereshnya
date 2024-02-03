package ru.ac.phyche.chereshnya;

import org.apache.commons.lang3.SystemUtils;

public class PythonRunner {
	public static void runPython(String script, String param) {
		String s = "";
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
			s = "./python/bin/python3";
		}
		if (SystemUtils.IS_OS_WINDOWS) {
			s = "./python/python.exe";
		}
		ProcessBuilder p = new ProcessBuilder(s, script, param).inheritIO();
		try {
			Process pr = p.start();
			pr.waitFor();
		} catch (Exception e) {
			throw (new RuntimeException(e.getMessage()));
		}
	}
}
