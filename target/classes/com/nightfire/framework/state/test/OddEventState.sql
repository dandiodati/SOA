-- 
-- Create one account and supplier that corresponding to the om_createOrder_in.xml 
--
-- delete from StateMachineMap;
-- delete from StateTransitionRule;

INSERT INTO StateMachineMap (StateMachineName, StateName, StateClassName) values ('OddEven', 'Zero', 'com.nightfire.framework.state.test.StateZero');
INSERT INTO StateMachineMap (StateMachineName, StateName, StateClassName) values ('OddEven', 'One', 'com.nightfire.framework.state.test.StateOne');
INSERT INTO StateMachineMap (StateMachineName, StateName, StateClassName) values ('OddEven', 'Two', 'com.nightfire.framework.state.test.StateTwo');
INSERT INTO StateMachineMap (StateMachineName, StateName, StateClassName) values ('OddEven', 'Three', 'com.nightfire.framework.state.test.StateThree');
INSERT INTO StateMachineMap (StateMachineName, StateName, StateClassName) values ('OddEven', 'Four', 'com.nightfire.framework.state.test.StateFour');

INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Zero', 'one', 'One');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Zero', 'three', 'Three');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'One', 'zero', 'Zero');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'One', 'two', 'Two');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'One', 'four', 'Four');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Two', 'one', 'One');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Two', 'three', 'Three');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Three', 'zero', 'Zero');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Three', 'two', 'Two');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Three', 'four', 'Four');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Four', 'one', 'One');
INSERT INTO StateTransitionRule (StateMachineName, FromStateName, StateEvent, ToStateName) values ('OddEven', 'Four', 'three', 'Three');

commit;

