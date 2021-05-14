"""
Generates an api diff from one commit to another.  This script relies on gitpython and similarly require git
installed on the system.  This script also requires python 3.

This script can be called as follows:

python apidiff.py <previous tag id> <latest tag id> -r <repo path> -o <output path>

If the '-o' flag is not specified, this script will create a folder at apidiff_output in the same directory as the
script.  For full list of options call:

python apidiff.py -h
"""

import os
import subprocess
import sys
import time
from pathlib import Path
from typing import Tuple, Iterator, List

import argparse as argparse
from git import Repo, Blob, Tree

"""
These are exit codes for jdiff:
return code 1   = error in jdiff
return code 100 = no changes
return code 101 = compatible changes
return code 102 = incompatible changes
"""
NO_CHANGES = 100
COMPATIBLE = 101
NON_COMPATIBLE = 102
ERROR = 1


def compare_xml(jdiff_path: str, root_dir: str, output_folder: str, oldapi_folder: str,
                newapi_folder: str, api_file_name: str, log_path: str) -> int:
    """
    Compares xml generated by jdiff using jdiff.
    :param jdiff_path: Path to jdiff jar.
    :param root_dir: directory for output .
    :param output_folder: Folder for diff output.
    :param oldapi_folder: Folder name of old api (i.e. release-4.10.2).
    :param newapi_folder: Folder name of new api (i.e. release-4.10.2).
    :param api_file_name: Name of xml file name (i.e. if output.xml, just 'output')
    :param log_path: Path to log file.
    :return: jdiff exit code.
    """
    jdiff_parent = os.path.dirname(jdiff_path)

    null_file = fix_path(os.path.join(jdiff_parent, "lib", "Null.java"))

    # comments are expected in a specific place
    make_dir(os.path.join(root_dir,
                          output_folder,
                          f"user_comments_for_{oldapi_folder}",
                          f"{api_file_name}_to_{newapi_folder}"))

    log = open(log_path, "w")
    cmd = ["javadoc",
           "-doclet", "jdiff.JDiff",
           "-docletpath", fix_path(jdiff_path),
           "-d", fix_path(output_folder),
           "-oldapi", fix_path(os.path.join(oldapi_folder, api_file_name)),
           "-newapi", fix_path(os.path.join(newapi_folder, api_file_name)),
           "-script",
           null_file]

    code = None
    try:
        jdiff = subprocess.Popen(cmd, stdout=log, stderr=log, cwd=root_dir)
        jdiff.wait()
        code = jdiff.returncode
    except Exception as e:
        log_and_print(log, f"Error executing javadoc: {str(e)}\nExiting...")
        exit(1)
    log.close()

    print(f"Compared XML for {oldapi_folder} {newapi_folder}")
    if code == NO_CHANGES:
        print("  No API changes")
    elif code == COMPATIBLE:
        print("  API Changes are backwards compatible")
    elif code == NON_COMPATIBLE:
        print("  API Changes are not backwards compatible")
    else:
        print("  *Error in XML, most likely an empty module")
    sys.stdout.flush()
    return code


def gen_xml(jdiff_path: str, output_path: str, log_output_path: str, src: str, packages: List[str]):
    """
    Uses jdiff to generate an xml representation of the source code.
    :param jdiff_path: Path to jdiff jar.
    :param output_path: Path to output path of diff.
    :param log_output_path: The log output path.
    :param src: The path to the source code.
    :param packages: The packages to process.
    """
    make_dir(output_path)

    log = open_log_file(log_output_path)
    log_and_print(log, f"Generating XML for: {src} outputting to: {output_path}")
    cmd = ["javadoc",
           "-doclet", "jdiff.JDiff",
           "-docletpath", fix_path(jdiff_path),
           "-apiname", fix_path(output_path),
           "-sourcepath", fix_path(src)]
    cmd = cmd + packages
    try:
        jdiff = subprocess.Popen(cmd, stdout=log, stderr=log)
        jdiff.wait()
    except Exception as e:
        log_and_print(log, f"Error executing javadoc {str(e)}\nExiting...")
        exit(1)

    log_and_print(log, f"Generated XML for: " + str(packages))
    log.close()
    sys.stdout.flush()


def _list_paths(root_tree: Tree, src_folder, path: Path = None) -> Iterator[Tuple[str, Blob]]:
    """
    Given the root path to serve as a prefix, walks the tree of a git commit returning all files and blobs.
    Repurposed from: https://www.enricozini.org/blog/2019/debian/gitpython-list-all-files-in-a-git-commit/
    Args:
        root_tree: The tree of the commit to walk.
        src_folder: relative path in repo to source folder that will be copied.
        path: The path to use as a prefix.
    Returns: A tuple iterator where each tuple consists of the path as a string and a blob of the file.
    """
    for blob in root_tree.blobs:
        next_path = Path(path) / blob.name if path else blob.name
        if Path(src_folder) in Path(next_path).parents:
            ret_item = (next_path, blob)
            yield ret_item
    for tree in root_tree.trees:
        next_path = Path(path) / tree.name if path else tree.name
        yield from _list_paths(tree, src_folder, next_path)


def _get_tree(repo_path: str, commit_id: str) -> Tree:
    """
    Retrieves the git tree that can be walked for files and file content at the specified commit.
    Args:
        repo_path: The path to the repo or a child directory of the repo.
        commit_id: The commit id.
    Returns: The tree.
    """
    repo = Repo(repo_path, search_parent_directories=True)
    commit = repo.commit(commit_id.strip())
    return commit.tree


def copy_commit_paths(repo_path, commit_id, src_folder, output_folder):
    """
    Copies all files located within a repo in the folder 'src_folder' to 'output_folder'.
    :param repo_path: The path to the repo.
    :param commit_id: The commit id.
    :param src_folder: The relative path in the repo to the source folder.
    :param output_folder: The output folder where the source will be copied.
    """
    tree = _get_tree(repo_path, commit_id)
    for rel_path, blob in _list_paths(tree, src_folder):
        output_path = os.path.join(output_folder, os.path.relpath(rel_path, src_folder))
        parent_folder = os.path.dirname(output_path)
        make_dir(parent_folder)
        output_file = open(output_path, 'w')
        output_file.write(blob.data_stream.read().decode('utf-8'))
        output_file.close()


def open_log_file(log_path):
    """
    Opens a path to a lof file for appending.  Creating directories and log file as necessary.
    :param log_path: The path to the log file.
    :return: The log file opened for writing.
    """
    if not os.path.exists(log_path):
        make_dir(os.path.dirname(log_path))
        Path(log_path).touch()

    return open(log_path, 'a+')


def fix_path(path):
    """
    Generates a path that is escaped from cygwin paths if present.
    :param path: Path (possibly including cygdrive).
    :return: The normalized path.
    """
    if "cygdrive" in path:
        new_path = path[11:]
        return "C:/" + new_path
    else:
        return path


def log_and_print(log, message):
    """
    Creates a log entry and prints to stdout.
    :param log: The log file object.
    :param message: The string to be printed.
    """
    time_stamp = time.strftime('%Y-%m-%d %H:%M:%S')
    print(f"{time_stamp}: {message}")
    log.write(f"{time_stamp}: {message}\n")


def make_dir(dir_path: str):
    """
    Create the given directory, if it doesn't already exist.
    :param dir_path: The path to the directory.
    :return: True if created.
    """
    try:
        if not os.path.isdir(dir_path):
            os.makedirs(dir_path)
        if os.path.isdir(dir_path):
            return True
        return False
    except IOError:
        print("Exception thrown when creating directory: " + dir_path)
        return False


def run_compare(output_path: str, jdiff_path: str, repo_path: str, src_rel_path: str, prev_commit_id: str,
                latest_commit_id: str, packages: List[str]):
    """
    Runs a comparison of the api between two different commits/branches/tags of the same repo generating a jdiff diff.
    :param output_path: The output path for artifacts.
    :param jdiff_path: The path to the jdiff jar.
    :param repo_path: The path to the repo.
    :param src_rel_path: The relative path in the repo to the source directory.
    :param prev_commit_id: The previous commit/branch/tag id.
    :param latest_commit_id: The latest commit/branch/tag id.
    :param packages: The packages to be considered for the api diff.
    """
    log_path = os.path.join(output_path, "messages.log")
    output_file_name = "output"
    diff_dir = "diff"
    src_folder = "src"

    for commit_id in [prev_commit_id, latest_commit_id]:
        src_copy = os.path.join(output_path, src_folder, commit_id)
        copy_commit_paths(repo_path, commit_id, src_rel_path, src_copy)
        gen_xml(jdiff_path, os.path.join(output_path, commit_id, output_file_name), log_path, src_copy, packages)

    # compare the two
    compare_xml(jdiff_path, output_path, os.path.join(output_path, diff_dir),
                prev_commit_id, latest_commit_id, output_file_name, log_path)


def main():
    parser = argparse.ArgumentParser(description="Generates a jdiff diff of the java api between two commits in a "
                                                 "repo.",
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument(dest='prev_commit', type=str, help=r'The git commit id/branch/tag to be used for the first '
                                                           r'commit')
    parser.add_argument(dest='latest_commit', type=str, help=r'The git commit id/branch/tag to be used for the latest '
                                                             r'commit')
    parser.add_argument('-r', '--repo', dest='repo_path', type=str, required=True,
                        help='The path to the repo.  If not specified, path of script is used.')

    parser.add_argument('-o', '--output', dest='output_path', type=str, required=False,
                        help='The location for output of all artifacts.  Defaults to an output folder in same directory'
                             'as script')
    parser.add_argument('-s', '--src', dest='src_rel_folder', type=str, required=False, default="bindings/java/src",
                        help='The relative path within the repo of the src folder.')
    # list of packages can be specified like this:
    # https://stackoverflow.com/questions/15753701/how-can-i-pass-a-list-as-a-command-line-argument-with-argparse
    parser.add_argument('-p', '--packages', dest='packages', nargs='+', required=False,
                        default=["org.sleuthkit.datamodel"], help='The packages to consider in api diff.')
    parser.add_argument('-j', '--jdiff', dest='jdiff_path', type=str, required=False,
                        help='The packages to consider in api diff.')

    args = parser.parse_args()
    script_path = os.path.dirname(os.path.realpath(__file__))
    repo_path = args.repo_path if args.repo_path else script_path
    output_path = args.output_path if args.output_path else os.path.join(script_path, "apidiff_output")
    jdiff_path = args.jdiff_path if args.jdiff_path else os.path.join(script_path,
                                                                      "thirdparty/jdiff/v-custom/jdiff.jar")
    run_compare(output_path=output_path,
                jdiff_path=jdiff_path,
                repo_path=repo_path,
                packages=args.packages,
                src_rel_path=args.src_rel_folder,
                prev_commit_id=args.prev_commit,
                latest_commit_id=args.latest_commit)


main()