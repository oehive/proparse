/* unexpected AST node: FILENAME */
OS-COPY "Consultingwerk/ProparseIssues/os-copy.p" "target.txt" .


/* unexpected token: VALUE */
def stream ausgabe.
OUTPUT STREAM ausgabe TO PRINTER VALUE(SESSION:PRINTER-NAME).


/* unexpected AST node: PERIOD */
DEFINE VARIABLE oForm AS Progress.Windows.Form NO-UNDO .
WAIT-FOR System.Windows.Forms.Application:Run (oForm) .


/* unexpected token: DYNAMIC-INVOKE */
def var h1 as handle.
dynamic-invoke(new progress.lang.object(), "yada", input-output dataset-handle h1, "b").


/* unexpected token: ( */
def var DataAccessObject as progress.lang.object.
def var DataAccessName as char.
def var DatasetHandle as handle.
THIS-OBJECT:DataAccessObject = 
        DYNAMIC-NEW (THIS-OBJECT:DataAccessName)
                    (THIS-OBJECT:DatasetHandle) .


/* unexpected AST node: FINAL */
method public final void whatever():
end method.
