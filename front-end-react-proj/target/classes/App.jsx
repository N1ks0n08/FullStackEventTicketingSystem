import { useState } from 'react'

function CounterSetup() {
  const [count, updateCount] = useState(0);

  function incrementCount() {
    updateCount(count + 1);
  }
  function decrementCount() {
    updateCount(count - 1);
  }

  return (
    <>
      <p>Current count: {count}</p>
      <button onClick={incrementCount}>Increment Count</button>
      <button onClick={decrementCount}>Decrement Count</button>
    </>
  );
}

function loeginInfoSetup() {
  
}

function App() {
  return (
    <>
      <h1> Welcome! </h1>
      <CounterSetup/>
    </>
  )
}

export default App
