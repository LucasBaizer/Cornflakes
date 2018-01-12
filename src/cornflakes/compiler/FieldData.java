package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

public class FieldData {
	private List<GenericType> genericTypes = new ArrayList<>();
	private String name;
	private String type;
	private Object proposedData;
	private boolean isGeneric;
	private int modifiers;

	public FieldData(String name, String type, int mods) {
		this.name = name;
		this.type = type;
		this.modifiers = mods;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public boolean hasModifier(int mod) {
		return (this.modifiers & mod) == mod;
	}

	public Object getProposedData() {
		return proposedData;
	}

	public void setProposedData(Object proposedData) {
		this.proposedData = proposedData;
	}

	@Override
	public String toString() {
		return name + ": " + type;
	}

	public List<GenericType> getGenericTypes() {
		return genericTypes;
	}

	public void setGenericTypes(List<GenericType> genericTypes) {
		this.genericTypes = genericTypes;
	}

	public boolean isGeneric() {
		return isGeneric;
	}

	public void setGeneric(boolean isGeneric) {
		this.isGeneric = isGeneric;
	}
}
