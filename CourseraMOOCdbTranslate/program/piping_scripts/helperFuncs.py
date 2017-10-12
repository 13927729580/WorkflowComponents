from utilities import dbfrom utilities import loggerfrom datetime import datetimeimport osimport reimport sqlparseimport warningsimport subprocessdef makeFeatureTables(vars):    preprocessing_files = [        [         'create_longitudinal_features.sql',         ['moocdb'],         [vars['target']['db']]        ],        [         'create_feature_extractions.sql',         ['moocdb'],         [vars['target']['db']]        ],        [         'create_user_longitudinal_feature_values.sql',         ['moocdb'],         [vars['target']['db']]        ],        [         'create_models_table.sql',         ['moocdb'],         [vars['target']['db']]         ],        [         'create_experiments_table.sql',         ['moocdb'],         [vars['target']['db']]        ]    ]    dirName = "preprocess_sql"    for fileName, toBeReplaced, replaceBy in preprocessing_files:        fileNameWithPath = os.path.dirname(os.path.realpath(__file__)) + "/" + dirName + '/' + fileName        vars['logger'].Log(vars, "Adding feature tables to {} with file {}".format(vars['target']['db'], fileNameWithPath))        newFile = replaceWordsInFile(fileNameWithPath, toBeReplaced, replaceBy, vars)        with warnings.catch_warnings():            warnings.filterwarnings('ignore', 'unknown table')            #warnings.simplefilter('ignore')            executeSQL(newFile, vars, vars['target']['host'], vars['target']['user'], vars['target']['password'], vars['target']['port'], vars['target']['db'])                 populateLongitudinalFeatures(vars)def restoreDB (vars, dbName, sourceFileName):    output = subprocess.call(['mysql', dbName, '--user={}'.format(vars['source']['user']), '--password={}'.format(vars['source']['password']), '-e', 'source {}'.format(sourceFileName)])    vars['logger'].Log(vars, "Restored database: {}; source SQL file: {}; output: {}.".format(dbName, sourceFileName, output))def executeSQL(command, vars, host, user, password, port, dbName):    #split commands by \n    commands = command.split("\n")    #remove comments and whitespace"    commands = [x for x in commands if x.lstrip()[0:2] != '--']    commands = [re.sub('\r','',x) for x in commands if x.lstrip() != '\r']    command = ' '.join(commands)    statements = sqlparse.split(command)    (target_con, target_cur) = db.connect(host, user, password, port, dbName)    for statement in statements:        if sqlparse.parse(statement):            db.Execute(target_cur, statement)    target_cur.close()    target_con.close()    return Truedef replaceWordsInFile(fileName, toBeReplaced, replaceBy, vars):    # toBeReplaced and replaceBy must be two string lists of same size    txt = open(fileName, 'r').read()    if len(toBeReplaced)!=len(replaceBy):        vars['logger'].Log(vars, "CAREFUL: sizes must be the same")        return    else:        for i in range(0,len(toBeReplaced)):            (newTxt, instances) = re.subn(re.escape(toBeReplaced[i]), replaceBy[i], txt)            #vars['logger'].Log(vars, "Number of instances changed: {}".format(instances))            txt = newTxt        return txtdef populateLongitudinalFeatures(vars):    (target_con, target_cur) = db.connect(vars['target']['host'], vars['target']['user'], vars['target']['password'], vars['target']['port'], vars['target']['db'])    insertion = '''INSERT INTO `%s`.`longitudinal_features`                                        (`longitudinal_feature_id`,                                         `longitudinal_feature_name`,                                         `longitudinal_feature_description`)                           VALUES ''' % vars['target']['db'];    values = [insertion]    features = featureDict    for feature_id in features.keys():        name = features[feature_id]['name']        description = features[feature_id]['desc']        values.append("(%s, '%s','%s')," % (feature_id,name,description))    values[-1] = values[-1][:-1]    values.append(';')    values = "".join(values)    db.Execute(target_cur, values)    target_cur.close()    target_con.close()    return Truedef getMinSubmissionTimestamp(vars):    # DB connections    # --------------    t = vars['target']    core_db_selector = db.Selector(t['host'], t['user'], t['password'], t['port'], t['db'])        rows = core_db_selector.query('''SELECT MIN(submission_timestamp) as min_sub_time FROM `%s`.submissions ''' % vars['target']['db'])    return rows[0]['min_sub_time']    def getFeatureNamesInOneLine(vars):    # DB connections    # --------------    t = vars['target']    core_db_selector = db.Selector(t['host'], t['user'], t['password'], t['port'], t['db'])    rows = core_db_selector.query('''SELECT longitudinal_feature_name FROM `%s`.longitudinal_features ''' % vars['target']['db'])    featureNames = []    for row in rows:        featureNames.append(row['longitudinal_feature_name'])    return '\t'.join(featureNames)###################################### FEATURE DICTIONARY ######################################################"""Contains a dictionary of all the potential features that can be extracted, and each feature in turn contains a dictionary of its salient features. This dictionary has the following format: \n.. code-block:: language    key -> feature_id    value -> a dictionary with the following key/value pairs:           KEY              VALUE       name:            descriptive name of feature,       filename:        filename containing feature script without extension,       extension:       either .sql or .py,       default:         default value of feature,       dependencies:    later features that depend on this extraction being run first,       desc:            description of what this feature is looking at"""featureDict = {     1:  {'name': "dropout",         'filename': 'populate_feature_1_dropout',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': "Whether or not the student has dropped out by this week (this is the label used in prediction)."},      2:  {'name': "sum_observed_events_duration",         'filename': 'populate_feature_2_sum_observed_events_duration',         'extension': '.sql',         'default': 0,         'dependencies':[10],         'desc': "Total time spent on each resource during the week. "},     3:  {'name': "number_of_forum_posts",         'filename': 'populate_feature_3_number_of_forum_posts',         'extension': '.sql',         'default': 0,         'dependencies':[103],         'desc': " Number of forum posts during the week."},     4:  {'name': "number_of_wiki_edits",         'filename': 'populate_feature_4_number_of_wiki_edits',         'extension': '.sql',         'default': 0,         'dependencies':[104],         'desc': "Number of wiki edits during the week. "},     5:  {'name': "average_length_of_forum_posts",         'filename': 'populate_feature_5_average_length_of_forum_posts',         'extension': '.sql',         'default': 0,         'dependencies':[105],         'desc': " Average length of forum posts during the week."},     6:  {'name': "distinct_attempts",         'filename': 'populate_feature_6_distinct_attempts',         'extension': '.sql',         'default': 0,         'dependencies':[11,111],         'desc': "Number of distinct problems attempted during the week. "},     7:  {'name': "number_of_attempts",         'filename': 'populate_feature_7_number_of_attempts',         'extension': '.sql',         'default': 0,         'dependencies':[209],         'desc': " Number of potentially non-distinct problem attempts during the week."},     8:  {'name': "distinct_problems_correct",         'filename': 'populate_feature_8_distinct_problems_correct',         'extension': '.sql',         'default': 0,         'dependencies':[10,11,110,111],         'desc': "Number of distinct problems correct during the week. "},     9:  {'name': "average_number_of_attempts",         'filename': 'populate_feature_9_average_number_of_attempts',         'extension': '.sql',         'default': 0,         'dependencies':[109,202,203],         'desc': "Average number of problem attempts during the week. "},     10: {'name': "sum_observed_events_duration_per_correct_problem",         'filename': 'populate_feature_10_sum_observed_events_duration_per_correct_problem',         'extension': '.sql',         'default': -1,         'dependencies':[110],         'desc': " Total time spent on all resources during the week (feat. 2) divided by number of correct problems (feat. 8)."},     11: {'name': "number_problem_attempted_per_correct_problem",         'filename': 'populate_feature_11_number_problem_attempted_per_correct_problem',         'extension': '.sql',         'default': -1,         'dependencies':[111],         'desc': " Number of problems attempted (feat. 6) divided by number of correct problems (feat. 8)."},     12: {'name': "average_time_to_solve_problem",         'filename': 'populate_feature_12_average_time_to_solve_problem',         'extension': '.sql',         'default': -1,         'dependencies':[112],         'desc': " Average of (max(attempt.timestamp) - min(attempt.timestamp)) for each problem during the week."},     13: {'name': "observed_event_timestamp_variance",         'filename': 'populate_feature_13_observed_event_timestamp_variance',         'extension':  '.py',         'default': 0,         'dependencies':[],         'desc': "Variance of a students observed event timestamps in one week. "},     14: {'name': "number_of_collaborations",         'filename': 'populate_feature_14_number_of_collaborations',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Number of collaborations during the week."},     15: {'name': "max_duration_resources",         'filename': 'populate_feature_15_max_duration_resources',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Duration of longest observed event"},        16: {'name': "lecture_event_duration",         'filename': 'populate_feature_16_lecture_event_duration',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Total time spent on all lecture-related resources during the week."},    #17: {'name': "sum_observed_events_book",    #   'filename': 'populate_feature_17_sum_observed_events_book',    #     'extension': '.sql',    #    'default': 0,    #     'dependencies':[],    #     'desc': " Total time spent on all book-related resources during the week."},     18: {'name': "wiki_event_duration",         'filename': 'populate_feature_18_wiki_event_duration',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Total time spent on all wiki-related resources during the week."},     19: {'name': "attempt_duration",         'filename': 'populate_feature_19_attempt_duration',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Total time spent on attempting questions for quizzes during the week."},    20: {'name': "number_of_lecture_events",         'filename': 'populate_feature_20_number_of_lecture_events',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Total number of lecture/video related observed events during the week."},    21: {'name': "number_of_test_submissions",         'filename': 'populate_feature_21_number_test_submission',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Total time spent on attempting questions for quizzes during the week."},                  103:{'name': "difference_feature_3",         'filename': 'populate_feature_103_difference_feature_3',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Number of forum posts in current week divided by number of forum posts in previous week (difference of feature 3)."},     104:{'name': "difference_feature_4",         'filename': 'populate_feature_104_difference_feature_4',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Number of wiki edits in current week divided by number of wiki edits in previous week (difference of feature 4)."},     105:{'name': "difference_feature_5",         'filename': 'populate_feature_105_difference_feature_5',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Average length of forum posts in current week divided by average length of forum posts in previous week, where number of forum posts in previous week is > 5 (difference of feature 5)."},     109:{'name': "difference_feature_9",         'filename': 'populate_feature_109_difference_feature_9',         'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Average number of attempts in current week divided by average number of attempts in previous week (difference of feature 9)."},     #110:{'name': "difference_feature_10",     #    'filename': 'populate_feature_110_difference_feature_10',     #    'extension': '.sql',     #    'default': 0,     #    'dependencies':[],     #    'desc': " (Total time spent on all resources during current week (feat. 2) divided by number of correct problems during current week (feat. 8)) divided by same thing from previous week (difference of feature 10)."},     #111:{'name': "difference_feature_11",      #       'filename': 'populate_feature_111_difference_feature_11',      #       'extension': '.sql',      #   'default': 0,      #   'dependencies':[],    #  'desc': " (Number of problems attempted (feat. 6) divided by number of correct problems (feat. 8)) divided by same thing from previous week (difference of feature 11)."},     112:{'name': "difference_feature_12",             'filename': 'populate_feature_112_difference_feature_12',             'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " (Average of (max(attempt.timestamp) - min(attempt.timestamp)) for each problem during current week) divided by same thing from previous week (difference of feature 12)."},     201:{'name': "number_of_forum_responses",             'filename': 'populate_feature_201_number_of_forum_responses',             'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Number of forum responses per week (also known as CF1)."},     202:{'name': "percentile_of_average_number_of_attempts",             'filename': 'populate_feature_202_percentile_of_average_number_of_attempts',             'extension':  '.py',         'default': 0,         'dependencies':[],         'desc': " Each students average number of attempts during the week (feat. 9) compared with other students as a percentile."},     203:{'name': "percent_of_average_number_of_attempts",             'filename': 'populate_feature_203_percent_of_average_number_of_attempts',             'extension':  '.py',         'default': 0,         'dependencies':[],         'desc': " Each students average number of attempts during the week (feat. 9) compared with other students as a percent of max."},     204:{'name': "pset_grade",             'filename': 'populate_feature_204_pset_grade',             'extension': '.sql',         'default': 0,         'dependencies':[205],         'desc': " Number of homework problems correct in a week divided by number of homework problems in the week."},     205:{'name': "pset_grade_over_time",             'filename': 'populate_feature_205_pset_grade_over_time',             'extension': '.sql',         'default': -1,         'dependencies':[],         'desc': " Pset grade from current week (feature 204) - avg(pset grade from previous week)."},     #206:{'name': "lab_grade",    #       'filename': 'populate_feature_206_lab_grade',    #         'extension': '.sql',    #    'default': 0,    #     'dependencies':[207],    #     'desc': " Number of lab problems correct in a week divided by number of lab problems in the week."},     #207:{'name': "lab_grade_over_time",      #       'filename': 'populate_feature_207_lab_grade_over_time',      #       'extension': '.sql',      #   'default': 0,      #   'dependencies':[],      #   'desc': " Lab grade from current week (feature 206) - avg(lab grade from previous week)."},     208:{'name': "attempts_correct",             'filename': 'populate_feature_208_attempts_correct',             'extension': '.sql',         'default': -1,         'dependencies':[209],         'desc': " Number of attempts (any type) that were correct during the week."},     209:{'name': "percent_correct_submissions",             'filename': 'populate_feature_209_percent_correct_submissions',             'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Percentage of total submissions that were correct (feature 208 / feature 7)."},     210:{'name': "average_predeadline_submission_time",             'filename': 'populate_feature_210_average_predeadline_submission_time',             'extension': '.sql',         'default': -1,         'dependencies':[],         'desc': " Average time between problem submissions and problem due date (in seconds)."},     301:{'name': "std_hours_working",             'filename': 'populate_feature_301_std_hours_working',             'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Standard deviation of the hours the user produces events and collaborations. Tries to capture how regular a student is with her schedule while doing a MOOC."},         #302:{'name': "time_to_react",    #        'filename': 'populate_feature_302_time_to_react',    #         'extension': '.sql',    #     'default': 0,    #     'dependencies':[],    #     'desc': " Average time in days a student takes to react when a new resource in posted. Tried to capture how fast a student is reacting to new content."},    303:{'name': "final_grade",            'filename': 'populate_feature_303_final_grade',             'extension': '.sql',         'default': 0,         'dependencies':[],         'desc': " Final grade for a student."},    }