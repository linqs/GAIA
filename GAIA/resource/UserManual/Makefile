# Simple Make file to compile the latex file

TEXFILE=gaia-manual

all: latex

latex:
	pdflatex $(TEXFILE)
	bibtex $(TEXFILE)
	pdflatex $(TEXFILE)
	pdflatex $(TEXFILE)

clean:
	rm -f *.out *.toc *.brf *.log *.blg *.aux *.bbl *.synctex.gz

