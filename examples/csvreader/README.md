## CSV ExternalFormatReader Example

This project contains a very simple example of how to use VDMJ external format parsers. These
are used to extract VDM content for document formats that are not supported by VDMJ implicitly,
for example from PDF files or ZIP files, or perhaps HTML.

The example in this project parses CSV lines as a list of VDM expressions, returning a simple
type schema for the data, plus a value of that type representing the file content. The schema
types include a CellType and invariants via three functions, xxxInvriant(xxx), which have to be
implemented by the caller to define any rules required for the data.

A reader class must implement the ExternalFormatReader interface. Its getText method must
return the *entire* encapsulated VDM content from the document in one read operation - this
usually means the method must "preread" the content into a local buffer, like a StringBuilder,
which is then used to generate a String's toCharArray().

To plug the new reader (or multiple readers) into VDMJ, the "vdmj.parser.external_readers" property
must be set to a CSV list of <suffix>=<class> pairs, like ".csv=examples.csvreader.CSVReader".
Note that the suffix is not case sensitive.
