#!/usr/bin/env python3
"""检查 spring-transactional-mq Java 代码是否符合项目基础规范。"""

import argparse
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple


PROJECT_ROOT = Path(__file__).resolve().parent.parent
PROJECT_PACKAGE = "org.rdlinux.transactionalmq"
DEFAULT_SCAN_ROOTS = tuple(
    sorted(PROJECT_ROOT.glob("spring-transactional-mq-*/src/main/java"))
)
EXCLUDED_DIRECTORY_NAMES = {".git", "target", "node_modules"}

Violation = Tuple[int, str]


def read_java_file(file_path: Path) -> Tuple[Optional[str], List[Violation]]:
    """读取 Java 文件并检查编码和换行符。"""
    violations: List[Violation] = []
    raw_content = file_path.read_bytes()
    if b"\r\n" in raw_content:
        violations.append((1, "使用CRLF换行符，需转为LF。"))
    try:
        return raw_content.decode("utf-8"), violations
    except UnicodeDecodeError:
        violations.append((1, "文件不是有效的UTF-8编码。"))
        return None, violations


def find_package_name(lines: Sequence[str]) -> str:
    """获取 Java 文件声明的包名。"""
    for line in lines:
        match = re.search(r"^\s*package\s+([\w.]+)\s*;", line)
        if match:
            return match.group(1)
    return ""


def annotation_parenthesis_balance(line: str) -> int:
    """计算注解行的括号变化，忽略字符串中的括号。"""
    without_strings = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
    return without_strings.count("(") - without_strings.count(")")


def is_annotation_region(
    lines: Sequence[str], start_index: int, end_index: int
) -> bool:
    """判断指定行区间是否完整由注解及其多行参数组成。"""
    in_annotation = False
    saw_annotation = False
    balance = 0
    for index in range(start_index, end_index):
        stripped_line = lines[index].strip()
        if not stripped_line:
            continue
        if not in_annotation:
            if not stripped_line.startswith("@"):
                return False
            saw_annotation = True
            balance = annotation_parenthesis_balance(stripped_line)
            in_annotation = balance > 0
            continue
        balance += annotation_parenthesis_balance(stripped_line)
        if balance <= 0:
            in_annotation = False
    return saw_annotation and not in_annotation


def is_annotation_gap(
    lines: Sequence[str], start_index: int, end_index: int
) -> bool:
    """判断注释与声明之间是否只有空行和完整注解。"""
    if start_index == end_index:
        return True
    return is_annotation_region(lines, start_index, end_index)


def find_javadoc(lines: Sequence[str], current_index: int) -> Optional[str]:
    """获取声明前的 JavaDoc；重写方法返回空字符串以表示无需重复注释。"""
    immediate_index = current_index - 1
    while immediate_index >= 0 and not lines[immediate_index].strip():
        immediate_index -= 1
    if immediate_index >= 0 \
            and lines[immediate_index].strip().startswith("@Override"):
        return ""

    for index in range(current_index - 1, -1, -1):
        stripped_line = lines[index].strip()
        if not stripped_line.endswith("*/"):
            continue

        comment_start = index
        while comment_start >= 0 and "/**" not in lines[comment_start]:
            comment_start -= 1
        if comment_start < 0:
            return None
        if not is_annotation_gap(lines, index + 1, current_index):
            return None
        if any(
            lines[annotation_index].strip().startswith("@Override")
            for annotation_index in range(index + 1, current_index)
        ):
            return ""
        return "\n".join(lines[comment_start:index + 1])

    for index in range(current_index - 1, -1, -1):
        if lines[index].strip().startswith("@Override") \
                and is_annotation_region(lines, index, current_index):
            return ""
    return None


def javadoc_after_annotation(lines: Sequence[str], current_index: int) -> bool:
    """判断JavaDoc是否位于注解之后，声明之前。"""
    index = current_index - 1
    while index >= 0 and not lines[index].strip():
        index -= 1
    if index < 0 or not lines[index].strip().endswith("*/"):
        return False

    comment_start = index
    while comment_start >= 0 and "/**" not in lines[comment_start]:
        comment_start -= 1
    if comment_start < 0:
        return False

    for index in range(comment_start - 1, -1, -1):
        if lines[index].strip().startswith("@") \
                and is_annotation_region(lines, index, comment_start):
            return True
    return False


def is_single_line_javadoc(javadoc: Optional[str]) -> bool:
    """判断 JavaDoc 是否为单行注释。"""
    if not javadoc:
        return False
    stripped_javadoc = javadoc.strip()
    return "\n" not in javadoc and stripped_javadoc.startswith("/**") \
        and stripped_javadoc.endswith("*/")


def is_malformed_multiline_javadoc(javadoc: Optional[str]) -> bool:
    """判断多行 JavaDoc 是否包含空行或重复的星号。"""
    if not javadoc or is_single_line_javadoc(javadoc):
        return False
    lines = javadoc.splitlines()
    if len(lines) < 3 or lines[0].strip() != "/**" \
            or lines[-1].strip() != "*/":
        return True
    return any(
        not line.strip().startswith("*")
        or line.strip().startswith("* *")
        for line in lines[1:-1]
    )


def find_extra_asterisk_javadoc_tags(lines: Sequence[str]) -> List[Violation]:
    """检查 JavaDoc 标签行是否在标准前缀后重复写入星号。"""
    violations: List[Violation] = []
    is_in_javadoc = False
    extra_asterisk_tag_pattern = re.compile(
        r"^\s*\*\s+\*\s+@(?:param|return|throws|exception|deprecated|see|since|author|version)\b"
    )
    for index, line in enumerate(lines):
        if "/**" in line:
            is_in_javadoc = True
        if is_in_javadoc and extra_asterisk_tag_pattern.search(line):
            violations.append(
                (
                    index + 1,
                    "JavaDoc标签行含有多余星号，应使用“* @标签 ...”格式。",
                )
            )
        if is_in_javadoc and "*/" in line:
            is_in_javadoc = False
    return violations


def has_inline_field_javadoc(line: str) -> bool:
    """判断属性声明是否把 JavaDoc 写在同一行。"""
    comment_start = line.find("/**")
    comment_end = line.find("*/", comment_start + 3)
    if comment_start < 0 or comment_end < 0:
        return False
    declaration = line[comment_end + 2:].strip()
    return bool(re.match(
        r"^(?:(?:public|protected|private|static|final|transient|volatile)\s+)+",
        declaration,
    )) and declaration.endswith(";")


def collect_method_signature(lines: Sequence[str], start_index: int) -> str:
    """收集可能跨行的方法或构造方法声明。"""
    signature_lines: List[str] = []
    parenthesis_balance = 0
    found_parenthesis = False
    index = start_index
    while index < len(lines):
        current_line = lines[index].strip()
        signature_lines.append(current_line)
        parenthesis_balance += current_line.count("(") - current_line.count(")")
        found_parenthesis = found_parenthesis or "(" in current_line
        if found_parenthesis and parenthesis_balance <= 0:
            break
        if len(signature_lines) >= 20:
            break
        index += 1
    return " ".join(signature_lines)


def split_parameters(parameters: str) -> List[str]:
    """按顶层逗号拆分方法参数。"""
    result: List[str] = []
    current: List[str] = []
    angle_balance = 0
    parenthesis_balance = 0
    bracket_balance = 0
    for character in parameters:
        if character == "<":
            angle_balance += 1
        elif character == ">":
            angle_balance = max(0, angle_balance - 1)
        elif character == "(":
            parenthesis_balance += 1
        elif character == ")":
            parenthesis_balance = max(0, parenthesis_balance - 1)
        elif character == "[":
            bracket_balance += 1
        elif character == "]":
            bracket_balance = max(0, bracket_balance - 1)

        if (
            character == ","
            and angle_balance == 0
            and parenthesis_balance == 0
            and bracket_balance == 0
        ):
            result.append("".join(current).strip())
            current = []
            continue
        current.append(character)

    final_parameter = "".join(current).strip()
    if final_parameter:
        result.append(final_parameter)
    return result


def extract_parameter_names(signature: str) -> List[str]:
    """从方法声明中提取参数名。"""
    start = signature.find("(")
    if start < 0:
        return []

    balance = 0
    end = -1
    for index in range(start, len(signature)):
        character = signature[index]
        if character == "(":
            balance += 1
        elif character == ")":
            balance -= 1
            if balance == 0:
                end = index
                break
    if end < 0:
        return []

    parameter_names: List[str] = []
    for parameter in split_parameters(signature[start + 1:end]):
        identifiers = re.findall(r"[A-Za-z_$][A-Za-z0-9_$]*", parameter)
        if identifiers:
            parameter_names.append(identifiers[-1])
    return parameter_names


def extract_method_name(signature: str) -> str:
    """从方法声明中提取方法名。"""
    match = re.search(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*\(", signature)
    return match.group(1) if match else ""


def method_requires_return_doc(signature: str) -> bool:
    """判断方法 JavaDoc 是否需要 @return。"""
    method_name = extract_method_name(signature)
    if not method_name:
        return False
    prefix = signature[:signature.find("(")]
    prefix = prefix[:prefix.rfind(method_name)].strip()
    modifiers = {
        "public",
        "protected",
        "private",
        "abstract",
        "static",
        "final",
        "synchronized",
        "native",
        "strictfp",
        "default",
    }
    if prefix and all(token in modifiers for token in prefix.split()):
        return False
    prefix = re.sub(
        r"^(?:(?:public|protected|private|abstract|static|final|synchronized|native|strictfp|default)\s+)+",
        "",
        prefix,
    )
    prefix = re.sub(r"^<[^>]+>\s*", "", prefix)
    return bool(prefix) and not re.search(r"\bvoid\s*$", prefix)


def check_jdk8_compatibility(line: str, line_number: int) -> List[Violation]:
    """检查项目明确禁止的 JDK 9 及以上语法和常见 API。"""
    rules = (
        (r"\brecord\s+[A-Za-z_$]", "JDK 1.8不支持record。"),
        (r"\bvar\s+[A-Za-z_$][A-Za-z0-9_$]*\s*=", "JDK 1.8不支持局部变量var。"),
        (r"\b(?:sealed|non-sealed)\s+(?:class|interface)\b", "JDK 1.8不支持sealed类型。"),
        (r"\bcase\b[^:;\n]*->", "JDK 1.8不支持switch表达式。"),
        (
            r"\b(?:List|Set|Map)\.(?:of|ofEntries|copyOf)\s*\(",
            "JDK 1.8不支持集合工厂或copyOf方法。",
        ),
        (r"\.\s*isBlank\s*\(", "JDK 1.8不支持String.isBlank。"),
        (r"\bPath\.of\s*\(", "JDK 1.8不支持Path.of，请使用Paths.get。"),
        (
            r"\bFiles\.(?:readString|writeString)\s*\(",
            "JDK 1.8不支持Files.readString/writeString。",
        ),
        (r"\bOptional\w*\.isEmpty\s*\(", "JDK 1.8不支持Optional.isEmpty。"),
        (
            r"import\s+java\.(?:net\.http|lang\.module|util\.concurrent\.Flow)\b",
            "引用了JDK 9及以上新增API，项目必须兼容JDK 1.8。",
        ),
    )
    return [
        (line_number, message)
        for pattern, message in rules
        if re.search(pattern, line)
    ]


def check_file(file_path: Path) -> List[Violation]:
    """检查单个 Java 文件。"""
    content, violations = read_java_file(file_path)
    if content is None:
        return violations

    lines = content.splitlines()
    violations.extend(find_extra_asterisk_javadoc_tags(lines))
    package_name = find_package_name(lines)
    is_project_code = any(
        is_relative_to(file_path, scan_root) for scan_root in DEFAULT_SCAN_ROOTS
    )

    if is_project_code and not (
        package_name == PROJECT_PACKAGE or package_name.startswith(PROJECT_PACKAGE + ".")
    ):
        violations.append((1, "项目Java源码包名必须使用org.rdlinux.transactionalmq根包。"))

    imports: Dict[str, int] = {}
    full_imports: Dict[str, str] = {}
    for line_number, line in enumerate(lines, 1):
        stripped_line = line.strip()
        if (
            not stripped_line.startswith("import ")
            or stripped_line.startswith("import static ")
            or stripped_line.endswith(".*;")
        ):
            continue
        match = re.search(r"import\s+([\w.]+)\.([\w]+)\s*;", stripped_line)
        if not match:
            continue
        import_package = match.group(1)
        class_name = match.group(2)
        full_import = re.sub(r"\s+", "", stripped_line)

        if import_package == package_name:
            violations.append((line_number, "同包下的类无需import: {}，请移除。".format(class_name)))
            continue
        if class_name not in imports:
            imports[class_name] = line_number
            full_imports[class_name] = full_import
        elif full_imports[class_name] == full_import:
            violations.append((line_number, "重复的import类: {}，请移除。".format(class_name)))
        else:
            violations.append((line_number, "存在同名的import类: {}，请检查。".format(class_name)))

    text_no_comments_strings = re.sub(
        r'/\*.*?\*/|//.*?$|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
        "",
        content,
        flags=re.MULTILINE | re.DOTALL,
    )
    text_without_imports = re.sub(
        r"^\s*import\s+(?:static\s+)?[\w.*]+\s*;",
        "",
        text_no_comments_strings,
        flags=re.MULTILINE,
    )

    for index, line in enumerate(lines):
        line_number = index + 1
        stripped_line = line.strip()
        line_no_strings = re.sub(r'"[^"\\]*(?:\\.[^"\\]*)*"', '""', stripped_line)
        line_no_strings = re.sub(r"//.*$", "", line_no_strings)
        if stripped_line.startswith(("//", "*", "/*")):
            line_no_strings = ""

        if is_project_code:
            if '"""' in stripped_line:
                violations.append((line_number, "JDK 1.8不支持文本块。"))
            violations.extend(check_jdk8_compatibility(line_no_strings, line_number))

            if (
                not line_no_strings.startswith(("package ", "import ", "//", "*", "/*"))
                and re.search(
                    r"\b(?:[a-z_][a-z0-9_]*\.)+[A-Z][A-Za-z0-9_]*\b",
                    line_no_strings,
                )
            ):
                match = re.search(
                    r"\b(?:[a-z_][a-z0-9_]*\.)+[A-Z][A-Za-z0-9_]*\b",
                    line_no_strings,
                )
                if match and not match.group(0).startswith("java.lang."):
                    violations.append(
                        (
                            line_number,
                            "除java.lang和同包类外应显式import，不应直接使用全限定类名: {}".format(
                                match.group(0)
                            ),
                        )
                    )

            if (
                re.search(
                    r"new\s+(?!TypeReference\b)[A-Z][A-Za-z0-9_.]*\s*<[^>]*[A-Za-z0-9][^>]*>\s*\(",
                    line_no_strings,
                )
                and not re.search(
                    r"new\s+[A-Z][A-Za-z0-9_.]*\s*<[^>]*>\s*\(\s*\)\s*\{",
                    line_no_strings,
                )
            ):
                violations.append(
                    (line_number, "实例化泛型对象时使用菱形操作符，例如new ArrayList<>()。")
                )

            if re.search(
                r"\.\s*<[A-Za-z0-9_,\s?.]+>\s*[A-Za-z0-9_]+\s*\(",
                line_no_strings,
            ):
                violations.append(
                    (line_number, "调用泛型方法时无需显式指定泛型类型。")
                )

        type_match = re.match(
            r"^(?:(?:public|protected|private|abstract|static|final|strictfp)\s+)*"
            r"(?:class|interface|enum)\s+[A-Za-z_$]",
            stripped_line,
        )
        if type_match:
            type_javadoc = find_javadoc(lines, index)
            if javadoc_after_annotation(lines, index):
                violations.append(
                    (line_number, "类JavaDoc必须置于全部注解上方。")
                )
            if type_javadoc is None:
                violations.append((line_number, "缺少类JavaDoc注释。"))
            elif is_single_line_javadoc(type_javadoc) \
                    or is_malformed_multiline_javadoc(type_javadoc):
                violations.append(
                    (line_number, "类JavaDoc必须使用标准多行注释。")
                )

        if has_inline_field_javadoc(stripped_line):
            violations.append(
                (line_number, "属性JavaDoc必须单独占行并置于属性上方。")
            )

        starts_with_visibility = re.match(r"^(?:public|protected|private)\s+", stripped_line)
        first_parenthesis = stripped_line.find("(")
        equals_sign = stripped_line.find("=")
        is_field = bool(starts_with_visibility) and stripped_line.endswith(";") and (
            first_parenthesis < 0 or (equals_sign >= 0 and equals_sign < first_parenthesis)
        )
        if is_field:
            field_javadoc = find_javadoc(lines, index)
            if javadoc_after_annotation(lines, index):
                violations.append(
                    (line_number, "属性JavaDoc必须置于全部注解上方。")
                )
            if field_javadoc is None:
                violations.append((line_number, "缺少属性JavaDoc注释。"))
            elif is_single_line_javadoc(field_javadoc):
                violations.append(
                    (line_number, "属性JavaDoc必须使用多行注释。")
                )
            elif is_malformed_multiline_javadoc(field_javadoc):
                violations.append(
                    (line_number, "属性JavaDoc格式错误，应使用标准多行注释。")
                )

        is_method = (
            bool(starts_with_visibility)
            and first_parenthesis >= 0
            and (equals_sign < 0 or first_parenthesis < equals_sign)
            and " class " not in " {} ".format(stripped_line)
            and " interface " not in " {} ".format(stripped_line)
        )
        if is_method:
            javadoc = find_javadoc(lines, index)
            if javadoc_after_annotation(lines, index):
                violations.append(
                    (line_number, "方法JavaDoc必须置于全部注解上方。")
                )
            if javadoc is None:
                violations.append((line_number, "缺少方法JavaDoc注释。"))
            elif javadoc:
                if is_single_line_javadoc(javadoc) \
                        or is_malformed_multiline_javadoc(javadoc):
                    violations.append(
                        (
                            line_number,
                            "方法JavaDoc必须使用标准多行注释。",
                        )
                    )
                else:
                    signature = collect_method_signature(lines, index)
                    missing_parameters = [
                        parameter_name
                        for parameter_name in extract_parameter_names(signature)
                        if not re.search(
                            r"@param\s+{}\b".format(re.escape(parameter_name)),
                            javadoc,
                        )
                    ]
                    if missing_parameters:
                        violations.append(
                            (
                                line_number,
                                "方法JavaDoc缺少参数说明: {}。".format(
                                    ", ".join(missing_parameters)
                                ),
                            )
                        )
                    if method_requires_return_doc(signature) \
                            and "@return" not in javadoc:
                        violations.append(
                            (line_number, "方法JavaDoc缺少@return说明。")
                        )

    for class_name, line_number in imports.items():
        if not re.search(r"\b{}\b".format(re.escape(class_name)), text_without_imports):
            violations.append(
                (line_number, "未使用的import类: {}，请移除。".format(class_name))
            )

    return sorted(violations, key=lambda violation: violation[0])


def is_excluded(file_path: Path) -> bool:
    """判断文件是否位于无需检查的目录。"""
    return any(part in EXCLUDED_DIRECTORY_NAMES for part in file_path.parts)


def is_relative_to(file_path: Path, directory: Path) -> bool:
    """判断文件是否位于指定目录内，兼容 Python 3.8。"""
    try:
        file_path.resolve().relative_to(directory.resolve())
        return True
    except ValueError:
        return False


def collect_java_files(paths: Iterable[Path]) -> List[Path]:
    """收集指定文件或目录下的 Java 文件。"""
    java_files = set()
    for path in paths:
        if path.is_file() and path.suffix == ".java" and not is_excluded(path):
            java_files.add(path.resolve())
        elif path.is_dir():
            for java_file in path.rglob("*.java"):
                if not is_excluded(java_file):
                    java_files.add(java_file.resolve())
    return sorted(java_files)


def display_path(file_path: Path) -> str:
    """优先返回相对项目根目录的展示路径。"""
    try:
        return file_path.relative_to(PROJECT_ROOT).as_posix()
    except ValueError:
        return str(file_path)


def parse_args() -> argparse.Namespace:
    """解析命令行参数。"""
    parser = argparse.ArgumentParser(description="检查spring-transactional-mq Java代码规范")
    parser.add_argument(
        "paths",
        nargs="*",
        help="可选的Java文件或目录；默认扫描所有模块的src/main/java",
    )
    return parser.parse_args()


def main() -> int:
    """执行代码规范检查。"""
    args = parse_args()
    if args.paths:
        scan_paths = [
            Path(path).resolve() if Path(path).is_absolute() else (Path.cwd() / path).resolve()
            for path in args.paths
        ]
    else:
        scan_paths = list(DEFAULT_SCAN_ROOTS)

    missing_paths = [path for path in scan_paths if not path.exists()]
    if missing_paths:
        for missing_path in missing_paths:
            print("扫描路径不存在: {}".format(missing_path), file=sys.stderr)
        return 2

    java_files = collect_java_files(scan_paths)
    total_violations = 0
    print("检查文件数: {}".format(len(java_files)))
    for java_file in java_files:
        file_violations = check_file(java_file)
        if not file_violations:
            continue
        print("\n{}:".format(display_path(java_file)))
        for line_number, message in file_violations:
            print("  Line {}: {}".format(line_number, message))
            total_violations += 1

    if total_violations:
        print("\n共发现{}处潜在不规范情况，请参考项目规范检查修复。".format(total_violations))
        return 1

    print("未检测到明显的不规范情况。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
