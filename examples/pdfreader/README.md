## The ExternalFormatReader Example

This project contains a very simple example of how to use VDMJ external format parsers. These
are used to extract VDM content for document formats that are not supported by VDMJ implicitly,
for example from PDF files or ZIP files, or perhaps HTML.

The example in this project uses the Linux "pdftotext" command to process PDF files and extract
VDM source from between "%%VDM%%" markers. But any technique may be used to extract content.

A reader class must implement the ExternalFormatReader interface. Its getText method must
return the *entire* encapsulated VDM content from the document in one read operation - this
usually means the method must "preread" the content into a local buffer, like a StringBuilder,
which is then used to generate a String's toCharArray().

To plug the new reader (or multiple readers) into VDMJ, the "vdmj.parser.external_readers" property
or "vdmj.readers" resource file must be set to a list of <suffix>=<class> pairs, like
".pdf = examples.parser.PDFStreamReader". This will attempt to use the example PDFStreamReader class
for all files that end in ".pdf". Note that the suffix is not case sensitive.
