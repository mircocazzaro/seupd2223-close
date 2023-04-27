import io
import os
import json
import ijson

from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')


def generate_embedding(doc: str):
    return model.encode([doc])[0].tolist()


def generate_embedding_file(file_name: str, file_path: str, model_name: str):
    # Check if the model_name file not exists in the output directory
    if not os.path.exists(f'output/{model_name}'):
        os.makedirs(f'output/{model_name}')

    # Check if the file_name file exists in the output directory -> skip
    if os.path.exists(f'output/{model_name}/{file_name}'):
        pass

    # Open the JSON file
    with open(file_path, 'r') as f:
        # Use ijson to iterate over the JSON file
        parser = ijson.parse(f)

        # Use an io.StringIO object to buffer the modified JSON data
        buffer = io.StringIO()

        # Write the opening bracket to start the JSON array
        buffer.write('[')

        for prefix, event, value in parser:
            if event == 'start_map':
                obj = {}
                for prefix, event, value in parser:
                    if event == 'map_key':
                        key = value
                    elif event == 'string':
                        obj[key] = value
                    elif event == 'end_map':
                        break
                obj['contents'] = generate_embedding(obj['contents'])
                json_str = json.dumps(obj)
                buffer.write(json_str)

                # Write a comma to separate the objects (except for the last one)
                if prefix.endswith('.end_array'):
                    buffer.write(']')
                else:
                    buffer.write(',')

        # Open the output file in write mode
        with open(f'output/{model_name}/{file_name}', 'w') as f:
            # Write the contents of the buffer to the file
            f.write(buffer.getvalue())


files_count = 0
# iterate over the files in directory and generate embeddings for each file ends with .json
for file_name in os.listdir('/Users/farzad/Projects/uni/search_engine/publish/French/Documents/Json'):
    if file_name.endswith('.json'):
        print(f"{files_count} processing file: {file_name}")
        full_path = os.path.join('/Users/farzad/Projects/uni/search_engine/publish/French/Documents/Json', file_name)
        generate_embedding_file(file_name, full_path, 'all-MiniLM-L6-v2')
        files_count += 1
