package raven.config;

import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

/**
 * A class representing a value entry inside a {@linkplain ConfigClass}
 * 
 * @author Raven
 *
 */
public class ValueEntry extends FieldEntry {

	/**
	 * Indicator for a String-type
	 */
	public static final byte STRING = 0;
	/**
	 * Indicator for a float-type
	 */
	public static final byte FLOAT = 1;
	/**
	 * Indicator for a long-type
	 */
	public static final byte LONG = 2;
	/**
	 * Indicator for a SteamID - handled as long
	 */
	public static final byte STEAMID = 6;


	/**
	 * The data type of this value
	 */
	protected byte dataType;
	/**
	 * The String value of the value if it is of type {@link #STRING}
	 */
	protected String string;
	/**
	 * The float value of the value if it is of type {@link #FLOAT}
	 */
	protected float fl;
	/**
	 * The long value of this value if it is of type {@link #LONG}
	 */
	protected long lo;
	/**
	 * Indicating whether this is a nested entry (e.g. in an array)
	 */
	protected boolean nested;


	/**
	 * Creates a new instance of this class representing a String value
	 * 
	 * @param varName
	 *            The name of the variable this value is being assigned to or
	 *            <code>null</code> if this is a nested value inside an array
	 * @param content
	 *            The content to be represented by this entry
	 */
	public ValueEntry(String varName, String content) {
		super(varName);
		this.nested = varName == null;
		this.string = content;
		this.dataType = STRING;
	}

	/**
	 * Creates a new instance of this class representing a float value
	 * 
	 * @param varName
	 *            The name of the variable this value is being assigned to or
	 *            <code>null</code> if this is a nested value inside an array
	 * @param content
	 *            The content to be represented by this entry
	 */
	public ValueEntry(String varName, float content) {
		super(varName);
		this.nested = varName == null;
		this.fl = content;
		this.dataType = FLOAT;
	}

	/**
	 * Creates a new instance of this class representing a long value
	 * 
	 * @param varName
	 *            The name of the variable this value is being assigned to or
	 *            <code>null</code> if this is a nested value inside an array
	 * @param content
	 *            The content to be represented by this entry
	 */
	public ValueEntry(String varName, long content) {
		super(varName);
		this.nested = varName == null;
		this.lo = content;
		this.dataType = LONG;
	}

	@Override
	public byte getType() {
		return ConfigClassEntry.ASSIGNMENT;
	}

	/**
	 * Creates a {@linkplain ValueEntry} from a rapified config file. This method
	 * assumes that the given reader points right at the beginning of a
	 * value-entry-definition,
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @param isNested
	 *            Indicating whether the entry to be created is nested inside an
	 *            array (has no variable name)
	 * @return The created array
	 * @throws IOException
	 * @throws RapificationException
	 */
	protected static ValueEntry fromRapified(ByteReader reader, boolean isNested)
			throws IOException, RapificationException {
		byte dataType = reader.readByte();

		String varName = null;

		if (!isNested) {
			varName = reader.readString();

			if (varName.isEmpty()) {
				throw new RapificationException("Empty variable name!");
			}
		}

		switch (dataType) {
		case STRING:
			return new ValueEntry(varName, reader.readString());
		case FLOAT:
			return new ValueEntry(varName, reader.readFloat());
		case LONG:
		case STEAMID:
			return new ValueEntry(varName, reader.readInt32());
		default:
			throw new RapificationException("Unknown data type: " + dataType);
		}
	}

	/**
	 * Creates a {@linkplain ValueEntry} from a text config file. This method
	 * assumes that the given reader points right at the beginning of a
	 * value-entry-definition.
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @param varName
	 *            The variable name the represented value is being assigned to or
	 *            <code>null</code> if there's none
	 * @return The created entry
	 * @throws IOException
	 */
	protected static ValueEntry fromText(TextReader reader, String varName) throws IOException {
		int c = reader.peek();

		if (c == '"' || c == '\'') {
			// String
			return new ValueEntry(varName, reader.readString());
		}
		if (Character.isDigit(c) || c == '-' || c == '+') {
			float num = reader.readNumber();
			reader.consumeWhitespace();

			if (num == (long) num) {
				// must be a long
				return new ValueEntry(varName, (long) num);
			} else {
				// is indeed a float
				return new ValueEntry(varName, num);
			}
		}

		throw new IllegalStateException("Attempt to parse ValueEntry failed!");
	}

	/**
	 * Gets the data type of this assignment
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * Checks whether the given type matches the type of this entry
	 * 
	 * @param requiredType
	 *            The required type in order to access whatever method is calling
	 *            this one
	 */
	protected void checkAccess(byte requiredType) {
		if (requiredType != getDataType()) {
			throw new IllegalAccessError("Can't access method - required data type doesn't match!");
		}
	}

	/**
	 * Gets the string value that is being assigned. This method may only be called
	 * if {@link #getDataType()} returns {@link #STRING}
	 */
	public String getString() {
		checkAccess(STRING);
		return string;
	}

	/**
	 * Gets the float value that is being assigned. This method may only be called
	 * if {@link #getDataType()} returns {@link #FLOAT}
	 */
	public float getFloat() {
		checkAccess(FLOAT);
		return fl;
	}

	/**
	 * Gets the long value that is being assigned. This method may only be called if
	 * {@link #getDataType()} returns {@link #LONG}
	 */
	public long getLong() {
		checkAccess(LONG);
		return lo;
	}

	/**
	 * Checks whether this entry is a nested one (inside another array) that does
	 * not have a {@link #varName}
	 */
	public boolean isNested() {
		return nested;
	}

	@Override
	public String toString() {
		String str = "ValueEntry" + (isNested() ? "" : ": \"" + getVarName() + "\"") + " - ";

		switch (getDataType()) {
		case STRING:
			str += "String: \"" + getString() + "\"";
			break;
		case FLOAT:
			str += "Float: " + getFloat();
			break;
		case LONG:
			str += "LONG " + getLong();
			break;
		default:
			str += "type=" + getType();
		}

		return str;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ValueEntry)) {
			return false;
		}

		ValueEntry other = (ValueEntry) o;

		return this.nested && (this.varName == null ? other.varName == null : this.varName.equals(other.varName))
				&& (this.string == null ? other.string == null : this.string.equals(other.string))
				&& this.fl == other.fl && this.lo == other.lo;
	}

	@Override
	public String getFieldValueString() {
		switch (dataType) {
		case STRING:
			return "\"" + string + "\"";
		case FLOAT:
			return String.valueOf(fl);
		case LONG:
			return String.valueOf(lo);
		default:
			throw new IllegalStateException("Unknown datattype " + dataType);
		}
	}

}
