import io
import os
import json
import ijson

from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')


def generate_embedding(doc: str):
    return model.encode([doc])[0].tolist()


def generate_embedding_file(file_name: str):
    # Open the JSON file
    with open(file_name, 'r') as f:
        # Use ijson to iterate over the JSON file
        parser = ijson.parse(f)

        # Use an io.StringIO object to buffer the modified JSON data
        buffer = io.StringIO()

        for prefix, event, value in parser:
            if event == 'start_map':
                obj = {}
                keys = []
                for prefix, event, value in parser:
                    if event == 'map_key':
                        key = value
                        keys.append(key)
                    elif event == 'start_array':
                        obj[key] = []
                    elif event == 'string':
                        obj[key].append(value)
                    elif event == 'end_map':
                        break

                for key in keys:
                    emb_data = []
                    for item in obj[key]:
                        emb_data.append(generate_embedding(item))
                    obj[key] = emb_data

                json_str = json.dumps(obj)
                buffer.write(json_str)

        # Open the output file in write mode
        with open(f'embeded-{file_name}', 'w') as f:
            # Write the contents of the buffer to the file
            f.write(buffer.getvalue())


files_count = 0
# iterate over the files in directory and generate embeddings for each file ends with .json
generate_embedding_file('result.json')
