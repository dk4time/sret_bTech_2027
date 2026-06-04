const Home = () => {
  return (
    <div style={styles.container}>
      <h1>Food Delivery Orders</h1>

      <p>
        Welcome to the Orders Dashboard. Use the navigation above to explore
        features.
      </p>

      <ul>
        <li>View all valid orders</li>
        <li>Filter orders by restaurant</li>
        <li>Check analytics in stats</li>
      </ul>
    </div>
  );
};

const styles = {
  container: {
    padding: "30px",
  },
};

export default Home;
