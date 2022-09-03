# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'Orion'
copyright = '2022, lsteveol'
author = 'lsteveol'
release = '0.1'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration
source_suffix = ['.rst', '.md']
extensions = ['sphinx.ext.todo',
              'sphinx.ext.autosectionlabel',
              'rst2pdf.pdfbuilder',
              'm2r2']

templates_path = ['_templates']
exclude_patterns = []

todo_include_todos = True

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'sphinx_rtd_theme'
html_static_path = ['_static']

pdf_stylesheets = ['style.style']
pdf_style_path   = ['./']

pdf_documents = [('index', u'Orion', u'Orion Documentation', u'lsteveol'),]
