// $ANTLR 2.7.1: "simpleedi.g" -> "SimpleEDILexer.java"$

  package com.nightfire.adapter.converter.edi;

  import java.io.*;
  import java.util.Vector;
  import java.util.Hashtable;
  import java.util.List;
  import java.util.LinkedList;
 
public interface SimpleEDITokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int ENVELOPE = 4;
	int FUNCGROUP = 5;
	int TRANSACTION = 6;
	int CONTAINER = 7;
	int SEGMENT = 8;
	int ELEM = 9;
	int SEGTERM = 10;
	int LITERAL_ISA = 11;
	int LITERAL_IEA = 12;
	int LITERAL_GS = 13;
	int LITERAL_GE = 14;
	int LITERAL_ST = 15;
	int LITERAL_SE = 16;
	int SEP = 17;
}
