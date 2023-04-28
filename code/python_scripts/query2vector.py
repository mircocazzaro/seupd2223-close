import re
from sentence_transformers import SentenceTransformer

model_name = 'all-MiniLM-L6-v2'
model = SentenceTransformer(model_name)


def generate_embedding(doc: str):
    return model.encode([doc])[0].tolist()


# use train.trec file to expand queries
f = open("/Users/farzad/Projects/uni/search_engine/publish/French/Queries/train.trec", "r")
text = f.read()

# extract queries from train.trec file
pattern = r'<top>\s*<num>(.*?)<\/num>\s*<title>(.*?)<\/title>\s*<\/top>'
matches = re.findall(pattern, text, re.DOTALL)

results = open(f"output/{model_name}/train.trec", "a")  # append mode

for match in matches:
    num, title = match
    expanded_terms = generate_embedding(title)
    # write as the same format as train.trec
    results.write(f"<top>\n<num>{num}</num>\n<title>{' '.join(map(str, expanded_terms))}</title>\n</top>\n")
    # print(expanded_terms)

results.close()
