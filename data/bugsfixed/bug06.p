/* unexpected AST node: FILENAME */
OS-COPY "Consultingwerk/ProparseIssues/os-copy.p" "target.txt" .


/* unexpected token: VALUE */
def stream ausgabe.
OUTPUT STREAM ausgabe TO PRINTER VALUE(SESSION:PRINTER-NAME).


/* unexpected AST node: PERIOD */
DEFINE VARIABLE oForm AS Progress.Windows.Form NO-UNDO .
WAIT-FOR System.Windows.Forms.Application:Run (oForm) .

