package com.macaowater.entity;

import java.util.ArrayList;
import java.util.List;

public class SortLevel {
	
	public SortLevel addChild(String levelName, int pageNumber) {
		if (children == null) {
			children = new ArrayList<SortLevel>();
		}
		
		SortLevel child = null;
		boolean hasThatChild = false;
		for (SortLevel sl : children) {
			if (levelName.equals(sl.getLevelName())) {
				hasThatChild = true;
				child = sl;
				break;
			}
		}
		if (!hasThatChild) {
			child = new SortLevel(levelName);
			children.add(child);
		}
		
		if (child.getStart() == -1) {
			child.setStart(pageNumber);
		}
		
		child.setEnd(pageNumber);
		
		return child;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		if (children != null && children.size() > 0) {
			for (SortLevel sl : children) {
				result.append(sl.toString());
			}
		}
		
		if (parent != null) {
			result.append(parent.getLevelName() + "-");
		}
		result.append(levelName + "," + start + "," + end + "," + (end - start + 1) + "\n");
		
		return result.toString();
	}
	
	public SortLevel(String levelName) {
		this.levelName = levelName;
	}

	public String getLevelName() {
		return levelName;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}

	private SortLevel parent;
	private List<SortLevel> children;
	private String levelName;
	private int start = -1;
	private int end = -1;
}
